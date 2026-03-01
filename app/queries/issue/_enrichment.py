"""이슈 목록/상세 공통 enrichment 로직."""

import uuid
from dataclasses import dataclass, field

from sqlalchemy.orm import Session

from app.modules.file.mapper import to_file_item
from app.modules.file.schemas import FileItem
from app.modules.issue import repository as repo
from app.modules.issue.models import ChangeRequest, Issue
from app.modules.issue.schemas import (
    AssigneeSummary,
    LabelBadge,
    PartBadge,
)


@dataclass
class IssueEnrichment:
    """단일 이슈에 대한 enrichment 데이터."""

    created_by_name: str | None = None
    labels: list[LabelBadge] = field(default_factory=list)
    assignees: list[AssigneeSummary] = field(default_factory=list)
    reviewers: list[AssigneeSummary] = field(default_factory=list)
    parts: list[PartBadge] = field(default_factory=list)
    files: list[FileItem] = field(default_factory=list)
    comments_count: int = 0


def load_enrichments(
    db: Session,
    issues: list[Issue],
) -> dict[uuid.UUID, IssueEnrichment]:
    """이슈 목록에 대한 enrichment 데이터를 배치 조회하여 반환."""
    if not issues:
        return {}

    issue_ids = [i.id for i in issues]

    labels_map = repo.batch_load_labels(db, issue_ids)
    assignee_ids_map = repo.batch_load_assignee_ids(db, issue_ids)
    parts_map = repo.batch_load_parts(db, issue_ids)
    files_map = repo.batch_load_files(db, issue_ids)
    comment_counts = repo.batch_load_comment_counts(db, issue_ids)

    # CR 검토자 배치 조회 (CR만 해당)
    cr_ids = [i.id for i in issues if isinstance(i, ChangeRequest)]
    reviewer_ids_map = repo.batch_load_reviewer_ids(db, cr_ids) if cr_ids else {}

    # User 이름 일괄 조회
    all_user_ids: set[uuid.UUID] = set()
    for issue in issues:
        if issue.created_by:
            all_user_ids.add(issue.created_by)
    for ids in assignee_ids_map.values():
        all_user_ids.update(ids)
    for ids in reviewer_ids_map.values():
        all_user_ids.update(ids)
    user_names = repo.batch_load_user_names(db, list(all_user_ids))

    result: dict[uuid.UUID, IssueEnrichment] = {}
    for issue in issues:
        issue_labels = [
            LabelBadge(id=lb.id, name=lb.name, color=lb.color)
            for lb in labels_map.get(issue.id, [])
        ]
        issue_assignees = [
            AssigneeSummary(id=uid, full_name=user_names.get(uid, ""))
            for uid in assignee_ids_map.get(issue.id, [])
        ]
        issue_reviewers = [
            AssigneeSummary(id=uid, full_name=user_names.get(uid, ""))
            for uid in reviewer_ids_map.get(issue.id, [])
        ]
        issue_parts = [
            PartBadge(id=p.id, part_number=p.part_number, name=p.name)
            for p in parts_map.get(issue.id, [])
        ]
        issue_files = [
            to_file_item(f) for f in files_map.get(issue.id, [])
        ]

        result[issue.id] = IssueEnrichment(
            created_by_name=(
                user_names.get(issue.created_by) if issue.created_by else None
            ),
            labels=issue_labels,
            assignees=issue_assignees,
            reviewers=issue_reviewers,
            parts=issue_parts,
            files=issue_files,
            comments_count=comment_counts.get(issue.id, 0),
        )

    return result
