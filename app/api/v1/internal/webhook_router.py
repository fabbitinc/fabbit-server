"""내부 서비스 webhook 수신 라우터."""

from fastapi import APIRouter, Header

from app.core.exceptions import AppError
from app.infrastructure.drawing_converter_client import drawing_converter_client
from app.modules.drawing import service as drawing_service
from app.modules.drawing.schemas import (
    BatchConversionResultRequest,
    ConversionResultRequest,
)

router = APIRouter(
    prefix="/api/v1/internal/webhooks",
    tags=["webhooks"],
)

_converter = drawing_converter_client


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


@router.post("/drawing-converter/batch")
def receive_batch_drawing_conversion(
    req: BatchConversionResultRequest,
    authorization: str = Header(...),
):
    """Drawing Converter MSA의 배치 변환 완료 webhook을 수신.

    배치 변환 요청에 대한 결과를 일괄 수신하여 각 Drawing 레코드에 반영합니다.
    """
    token = authorization.removeprefix("Bearer ").strip()
    if not _converter.verify_secret(token):
        raise AppError(message="인증 실패", code="UNAUTHENTICATED")

    drawing_service.handle_batch_conversion_result(req)
    return {"status": "ok"}
