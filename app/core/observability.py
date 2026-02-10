"""OpenTelemetry 설정 - 분산 추적 및 로그 통합.

OTel-Native 전략:
- Trace: "어디로 갔는가" (자동 계측)
- Log: "무슨 생각을 했는가" (비즈니스 문맥)
"""

from opentelemetry import trace
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor, ConsoleSpanExporter
from opentelemetry.sdk.resources import Resource, SERVICE_NAME
from opentelemetry.exporter.otlp.proto.grpc.trace_exporter import OTLPSpanExporter
from opentelemetry.instrumentation.fastapi import FastAPIInstrumentor
from opentelemetry.instrumentation.sqlalchemy import SQLAlchemyInstrumentor

from app.core.config import settings


def get_current_trace_context() -> dict[str, str | None]:
    """현재 Span의 trace_id, span_id를 반환."""
    span = trace.get_current_span()
    if span.is_recording():
        ctx = span.get_span_context()
        return {
            "trace_id": format(ctx.trace_id, "032x"),
            "span_id": format(ctx.span_id, "016x"),
        }
    return {"trace_id": None, "span_id": None}


def setup_telemetry() -> None:
    """OpenTelemetry 초기화."""
    if not settings.otel_enabled:
        return

    # Resource 설정 (서비스 메타데이터)
    resource = Resource.create({
        SERVICE_NAME: settings.otel_service_name,
        "service.version": "0.1.0",
        "deployment.environment": "development" if settings.debug else "production",
    })

    # TracerProvider 생성
    provider = TracerProvider(resource=resource)

    # Exporter 설정
    if settings.otel_exporter_endpoint:
        headers = _parse_headers(settings.otel_exporter_headers)
        exporter = OTLPSpanExporter(
            endpoint=settings.otel_exporter_endpoint,
            headers=headers,
        )
    else:
        exporter = ConsoleSpanExporter()

    # BatchSpanProcessor로 비동기 전송
    processor = BatchSpanProcessor(exporter)
    provider.add_span_processor(processor)

    # Global TracerProvider 등록
    trace.set_tracer_provider(provider)


def instrument_app(app) -> None:
    """FastAPI 앱에 자동 계측 적용."""
    if not settings.otel_enabled:
        return

    FastAPIInstrumentor.instrument_app(
        app,
        excluded_urls="health,healthz,ready,readyz,metrics",
    )


def instrument_database(engine) -> None:
    """SQLAlchemy 엔진에 자동 계측 적용 (sync engine)."""
    if not settings.otel_enabled:
        return

    SQLAlchemyInstrumentor().instrument(
        engine=engine,
        enable_commenter=True,
    )


def _parse_headers(headers_str: str | None) -> dict[str, str]:
    """헤더 문자열을 딕셔너리로 파싱 ("key1=value1,key2=value2" 형식)."""
    if not headers_str:
        return {}

    headers = {}
    for pair in headers_str.split(","):
        if "=" in pair:
            key, value = pair.split("=", 1)
            headers[key.strip()] = value.strip()
    return headers


def get_tracer(name: str = __name__) -> trace.Tracer:
    """수동 span 생성용 Tracer 인스턴스 반환."""
    return trace.get_tracer(name)
