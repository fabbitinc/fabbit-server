"""내부 서비스 webhook 수신 라우터."""

from fastapi import APIRouter, Header

from app.core.exceptions import AppError
from app.infrastructure.drawing_converter_client import DrawingConverterClient
from app.modules.drawing.schemas import ConversionResultRequest
from app.modules.drawing import service as drawing_service

router = APIRouter(
    prefix="/api/v1/internal/webhooks",
    tags=["webhooks"],
)

_converter = DrawingConverterClient()


@router.post("/drawing-converter")
def receive_drawing_conversion(
    req: ConversionResultRequest,
    authorization: str = Header(...),
):
    """Drawing Converter MSA의 변환 완료 webhook을 수신.

    shared secret으로 인증하며, 변환 결과를 Drawing 레코드에 반영합니다.
    """
    token = authorization.removeprefix("Bearer ").strip()
    if not _converter.verify_secret(token):
        raise AppError(message="인증 실패", code="UNAUTHENTICATED")

    drawing_service.handle_conversion_result(req)
    return {"status": "ok"}
