"""Drawing Converter MSA HTTP 클라이언트."""

import hmac
import uuid

import httpx
from loguru import logger

from app.core.config import settings


class DrawingConverterClient:
    """DWG → PDF/썸네일 변환 요청을 Converter MSA에 전송."""

    def __init__(self) -> None:
        self._url = settings.drawing_converter_url
        self._secret = settings.drawing_converter_secret
        self._enabled = bool(self._url)

    @property
    def enabled(self) -> bool:
        return self._enabled

    def request_conversion(
        self,
        tenant_schema: str,
        callback_url: str,
        file_id: uuid.UUID,
        file_key: str,
        drawing_id: uuid.UUID,
    ) -> None:
        """Converter에 변환 요청 전송. url 미설정 시 스킵."""
        if not self._enabled:
            logger.debug("Drawing Converter 비활성화 — 변환 요청 스킵")
            return

        payload = {
            "file_key": file_key,
            "file_id": str(file_id),
            "drawing_id": str(drawing_id),
            "tenant_schema": tenant_schema,
            "callback_url": callback_url,
        }
        resp = httpx.post(
            f"{self._url}/api/v1/convert",
            json=payload,
            headers={"Authorization": f"Bearer {self._secret}"},
            timeout=10,
        )
        resp.raise_for_status()
        logger.info(
            "변환 요청 전송: file_id={file_id}",
            file_id=file_id,
        )

    def request_batch_conversion(
        self,
        tenant_schema: str,
        callback_url: str,
        items: list[dict],
    ) -> None:
        """Converter에 배치 변환 요청 전송.

        items: [{"file_id": uuid, "file_key": str, "drawing_id": uuid}, ...]
        """
        if not self._enabled:
            logger.debug("Drawing Converter 비활성화 — 배치 변환 요청 스킵")
            return

        payload = {
            "items": [
                {
                    "file_key": item["file_key"],
                    "file_id": str(item["file_id"]),
                    "drawing_id": str(item["drawing_id"]),
                }
                for item in items
            ],
            "tenant_schema": tenant_schema,
            "callback_url": callback_url,
        }
        resp = httpx.post(
            f"{self._url}/api/v1/convert/batch",
            json=payload,
            headers={"Authorization": f"Bearer {self._secret}"},
            timeout=30,
        )
        resp.raise_for_status()
        logger.info(
            "배치 변환 요청 전송: count={count}",
            count=len(items),
        )

    def verify_secret(self, token: str) -> bool:
        """Webhook 요청의 shared secret 검증."""
        if not self._secret:
            return False
        return hmac.compare_digest(token, self._secret)


drawing_converter_client = DrawingConverterClient()
