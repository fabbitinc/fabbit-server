"""라벨 도메인 모델 → Pydantic 응답 변환."""

from app.modules.label.models import Label
from app.modules.label.schemas import LabelResponse


def to_label_response(label: Label) -> LabelResponse:
    """Label 모델 → LabelResponse 변환."""
    return LabelResponse(
        id=label.id,
        project_id=label.project_id,
        name=label.name,
        description=label.description,
        color=label.color,
        created_at=label.created_at,
        created_by=label.created_by,
    )
