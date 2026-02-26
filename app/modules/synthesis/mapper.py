"""Synthesis 도메인 응답 변환 매퍼."""

from app.modules.synthesis.models import SynthesisJob
from app.modules.synthesis.schemas import SynthesisJobResponse


def to_job_response(job: SynthesisJob) -> SynthesisJobResponse:
    return SynthesisJobResponse(
        id=job.id,
        mapping_id=job.mapping_id,
        file_id=job.file_id,
        status=job.status,
        total_rows=job.total_rows,
        processed_rows=job.processed_rows,
        nodes_created=job.nodes_created,
        relationships_created=job.relationships_created,
        errors=job.errors,
        started_at=job.started_at,
        completed_at=job.completed_at,
        created_at=job.created_at,
    )
