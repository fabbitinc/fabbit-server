---
name: logging
description: |
  OTel-Native 로깅 전략 가이드.
  트리거: "로깅 추가", "로그 작성", "logger 사용", "로깅 전략", "add logging"
  OTel이 처리하는 것과 로그로 남겨야 할 것을 명확히 구분한다.
disable-model-invocation: false
user-invocable: false
---

# OTel-Native 로깅 전략

> **원칙**: "Trace는 '어디로 갔는가'를 보여주고, Log는 '무슨 생각을 했는가'를 보여준다."

## 핵심 개념

- **OTel**: 시스템의 건강검진 보고서 (성능, 흐름, 의존성)
- **Log**: 의사의 진료 소견서 (비즈니스 판단 근거, 특이사항)

---

## 1. 로그로 남기지 말 것 (OTel이 처리)

다음 패턴은 **삭제하거나 Span Event로 대체**:

```python
# ❌ BAD - 삭제할 것
logger.info("Starting process_order...")
logger.info(f"Processing took {elapsed}ms")
logger.info("Calling OpenAI API")
logger.info(f"Request: {request.method} {request.path}")
logger.info("Function completed successfully")
```

| 금지 항목 | 이유 | OTel 대체 |
|----------|------|-----------|
| 수행 시간 (`took 100ms`) | Span Duration | `span.end()` 시 자동 기록 |
| 진입/퇴장 (`Started/Finished`) | Span 시작/끝 | `with tracer.start_as_current_span()` |
| 단순 호출 (`Call API`) | HTTP Instrumentation | 자동 계측 |
| HTTP 메타데이터 | HTTP Trace | 자동 계측 |

---

## 2. 로그로 반드시 남길 것 (OTel이 모르는 비즈니스 문맥)

### 2.1 비즈니스 의사결정의 이유 (The "Why")

```python
# ✅ GOOD - 왜 이 경로를 선택했는지
logger.info(
    "결제 승인 거부: 잔액 부족",
    extra={"user_id": user.id, "balance": balance, "required": amount}
)

logger.info(
    "AI 폴백 로직 실행: 신뢰도 임계값 미달",
    extra={"confidence": 0.45, "threshold": 0.5, "model": model.value}
)

logger.warning(
    "재시도 정책 적용: 외부 서비스 일시 오류",
    extra={"attempt": 3, "max_attempts": 5, "service": "payment_gateway"}
)
```

### 2.2 데이터 특정적 디버깅 정보

```python
# ✅ GOOD - 특정 데이터의 예외 상황
logger.warning(
    "BOM 데이터 파싱 스킵: 비표준 스펙 형식",
    extra={"line": 15, "raw_value": row.get("spec"), "expected_format": "W x H x D"}
)

logger.info(
    "속성 매칭 실패: 유사 속성 없음",
    extra={"header": "Part No.", "candidates": [], "threshold": 0.7}
)
```

### 2.3 상태 변화의 핵심

```python
# ✅ GOOD - 상태 전이의 맥락
logger.info(
    "계정 상태 변경",
    extra={
        "account_id": account.id,
        "from_status": "PENDING",
        "to_status": "ACTIVE",
        "trigger": "admin_approval",
        "approved_by": admin_id,
    }
)
```

---

## 3. 레이어별 로깅 가이드

### 3.1 API/Middleware (Inbound Adapter)

**로깅 최소화** - 요청/응답은 OTel Trace로 대체

```python
# ❌ BAD
@router.post("/orders")
async def create_order(request: OrderRequest):
    logger.info(f"Received order request: {request}")  # 삭제
    result = await use_case.create(request)
    logger.info(f"Order created: {result.id}")  # 삭제
    return result

# ✅ GOOD - 보안 위반만 기록
@router.post("/orders")
async def create_order(request: OrderRequest):
    # OTel이 자동으로 HTTP span 생성
    return await use_case.create(request)

# 미들웨어에서 보안 위반 기록
async def rate_limit_middleware(request, call_next):
    if is_rate_limited(request.client.host):
        logger.warning(
            "Rate limit 초과",
            extra={"client_ip": request.client.host, "endpoint": request.url.path}
        )
        raise HTTPException(429)
    return await call_next(request)
```

### 3.2 Application Layer (UseCase)

**핵심 로직 분기점 기록** - "왜 이 길을 선택했는지"

```python
class OrderUseCase:
    @transactional
    async def create(self, request: CreateOrderRequest) -> Order:
        # 재고 확인 후 분기
        stock = await self._inventory.check(request.product_id)

        if stock.quantity < request.quantity:
            # ✅ 비즈니스 판단 근거 기록
            logger.info(
                "주문 생성 불가: 재고 부족",
                extra={
                    "product_id": request.product_id,
                    "requested": request.quantity,
                    "available": stock.quantity,
                }
            )
            raise OrderError.insufficient_stock()

        # 할인 정책 적용 분기
        discount = await self._pricing.calculate_discount(request.user_id)
        if discount.rate > 0:
            # ✅ 적용된 정책 기록
            logger.info(
                "할인 정책 적용",
                extra={
                    "user_id": request.user_id,
                    "discount_type": discount.type,
                    "rate": discount.rate,
                    "reason": discount.reason,
                }
            )

        order = Order(...)
        return await self._orders.save(order)
```

### 3.3 Outbound Adapter

**에러의 구체적 맥락** - OTel은 "실패"만, 로그는 "왜 실패"를 기록

```python
class OpenAiAdapter(AiAnalyzerPort):
    async def analyze_attributes(self, ...) -> tuple[...]:
        try:
            response = await self._client.chat.completions.create(...)
            return self._parse_response(response)

        except openai.RateLimitError as e:
            # ✅ 구체적 에러 맥락
            logger.error(
                "OpenAI API 호출 실패: 요청 한도 초과",
                extra={
                    "error_type": "rate_limit",
                    "retry_after": e.headers.get("retry-after"),
                    "model": model.value,
                }
            )
            raise

        except openai.APIError as e:
            # ✅ API가 전달한 구체적 메시지
            logger.error(
                "OpenAI API 오류",
                extra={
                    "error_code": e.code,
                    "error_message": str(e),
                    "request_id": e.request_id,
                }
            )
            raise
```

---

## 4. 로그 레벨 가이드

| Level | 용도 | 예시 |
|-------|------|------|
| `DEBUG` | 개발 중 디버깅 (프로덕션 OFF) | 상세 데이터 덤프 |
| `INFO` | 정상적인 비즈니스 이벤트 | 상태 변경, 정책 적용 |
| `WARNING` | 예상된 예외 상황 | 재시도, 폴백, 스킵 |
| `ERROR` | 처리 실패 (복구 불가) | 외부 서비스 오류, 데이터 손상 |

---

## 5. 구현 규칙

### 5.1 OTel Span 연동

모든 로그는 현재 Span Context와 연결되어야 함:

```python
# src/core/observability.py 에서 설정
from opentelemetry import trace
from loguru import logger

# Trace ID 자동 주입 설정
def trace_injection(record):
    span = trace.get_current_span()
    if span.is_recording():
        ctx = span.get_span_context()
        record["extra"]["trace_id"] = format(ctx.trace_id, "032x")
        record["extra"]["span_id"] = format(ctx.span_id, "016x")

logger.configure(patcher=trace_injection)
```

### 5.2 Extra 필드 사용

구조화된 데이터는 kwargs로 전달:

```python
# ❌ BAD - 문자열 포매팅 (검색/필터링 불가)
logger.info(f"User {user_id} balance is {balance}")

# ✅ GOOD - kwargs로 구조화된 extra 추가
logger.info("잔액 조회", user_id=user_id, balance=balance)
```

### 5.3 민감 정보 제외

```python
# ❌ BAD
logger.info("로그인 시도", extra={"email": email, "password": password})

# ✅ GOOD
logger.info("로그인 시도", extra={"email": email})
```

---

## 체크리스트

로그 코드 작성/리뷰 시 확인:

- [ ] OTel이 이미 처리하는 정보를 중복 로깅하지 않는가?
- [ ] 비즈니스 의사결정의 "이유"가 담겨 있는가?
- [ ] 구조화된 `extra` 필드를 사용하는가?
- [ ] 민감 정보(비밀번호, 토큰 등)가 제외되었는가?
- [ ] 적절한 로그 레벨을 사용하는가?
- [ ] Trace ID가 자동 연결되도록 설정되어 있는가?
