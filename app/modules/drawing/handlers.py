"""Drawing 도메인 이벤트 핸들러.

DB 변경 없이 로그만 남기는 안전한 핸들러.
"""

from loguru import logger

from app.core.event_bus import event_bus
from app.modules.drawing.events import (
    DrawingConversionCompleted,
    DrawingConversionFailed,
)


def _on_conversion_completed(event: DrawingConversionCompleted) -> None:
    logger.info(
        "[이벤트] 도면 변환 완료: drawing_id={drawing_id}",
        drawing_id=event.drawing_id,
    )


def _on_conversion_failed(event: DrawingConversionFailed) -> None:
    logger.warning(
        "[이벤트] 도면 변환 실패: drawing_id={drawing_id}",
        drawing_id=event.drawing_id,
    )


event_bus.subscribe(DrawingConversionCompleted, _on_conversion_completed)
event_bus.subscribe(DrawingConversionFailed, _on_conversion_failed)
