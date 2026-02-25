"""SynthesisJob 도메인 이벤트 핸들러.

DB 변경 없이 로그만 남기는 안전한 핸들러.
"""

from loguru import logger

from app.core.event_bus import event_bus
from app.modules.synthesis.events import SynthesisJobCompleted, SynthesisJobFailed


def _on_synthesis_completed(event: SynthesisJobCompleted) -> None:
    logger.info(
        "[이벤트] 합성 완료: job_id={job_id} 노드={nodes} 관계={rels}",
        job_id=event.job_id,
        nodes=event.nodes_created,
        rels=event.relationships_created,
    )


def _on_synthesis_failed(event: SynthesisJobFailed) -> None:
    logger.warning(
        "[이벤트] 합성 실패: job_id={job_id} errors={errors}",
        job_id=event.job_id,
        errors=event.errors,
    )


event_bus.subscribe(SynthesisJobCompleted, _on_synthesis_completed)
event_bus.subscribe(SynthesisJobFailed, _on_synthesis_failed)
