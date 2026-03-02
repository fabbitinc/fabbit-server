"""이슈 목록/상세 공통 enrichment 로직."""

import uuid
from dataclasses import dataclass, field

from sqlalchemy.orm import Session

from app.modules.file.mapper import to_file_item
from app.modules.file.schemas import FileItem
from app.modules.issue import repository as repo
from app.modules.issue.models import ChangeRequest, ChangeRequestReviewer, Issue
from app.modules.issue.schemas import (
    LabelBadge,
    LinkedChangeRequestBadge,
    LinkedIssueBadge,
    PartBadge,
    ReviewerSummary,
    TeamBadge,
)
from app.modules.user import mapper as user_mapper
from app.modules.user import repository as user_repo
from app.modules.user.schemas import UserSummary


@dataclass
class IssueEnrichment:
    """단일 이슈에 대한 enrichment 데이터."""

    created_by: UserSummary | None = None
    labels: list[LabelBadge] = field(default_factory=list)
    assignees: list[UserSummary] = field(default_factory=list)
    assigned_teams: list[TeamBadge] = field(default_factory=list)
    reviewers: list[ReviewerSummary] = field(default_factory=list)
    reviewer_teams: list[TeamBadge] = field(default_factory=list)
    parts: list[PartBadge] = field(default_factory=list)
    files: list[FileItem] = field(default_factory=list)
    comments_count: int = 0
    linked_issues: list[LinkedIssueBadge] = field(default_factory=list)
    linked_changes: list[LinkedChangeRequestBadge] = field(default_factory=list)


def _build_reviewer_summaries(
    reviewers: list[ChangeRequestReviewer],
    user_summary_map: dict[uuid.UUID, UserSummary],
) -> list[ReviewerSummary]:
    """ChangeRequestReviewer 목록 → ReviewerSummary 목록 변환."""
    result = []
    for r in reviewers:
        us = user_summary_map.get(r.user_id)
        if us:
            result.append(
                ReviewerSummary(
                    user_id=r.user_id,
                    full_name=us.full_name,
                    email=us.email,
                    review_status=r.review_status,
                    reviewed_at=r.reviewed_at,
                )
            )
    return result


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
    team_assignee_ids_map = repo.batch_load_team_assignee_ids(db, issue_ids)
    parts_map = repo.batch_load_parts(db, issue_ids)
    files_map = repo.batch_load_files(db, issue_ids)
    comment_counts = repo.batch_load_comment_counts(db, issue_ids)

    # CR 검토자 + 연결 이슈 배치 조회 (CR만 해당)
    cr_ids = [i.id for i in issues if isinstance(i, ChangeRequest)]
    reviewer_details_map = repo.batch_load_reviewer_details(db, cr_ids) if cr_ids else {}
    team_reviewer_ids_map = repo.batch_load_team_reviewer_ids(db, cr_ids) if cr_ids else {}
    linked_issues_map = repo.batch_load_linked_issues(db, cr_ids) if cr_ids else {}

    # Issue에 연결된 CR 배치 조회 (ISSUE만 해당)
    issue_only_ids = [i.id for i in issues if not isinstance(i, ChangeRequest)]
    linked_crs_map = repo.batch_load_linked_crs(db, issue_only_ids) if issue_only_ids else {}

    # Team 일괄 조회 → TeamBadge 매핑
    all_team_ids: set[uuid.UUID] = set()
    for ids in team_assignee_ids_map.values():
        all_team_ids.update(ids)
    for ids in team_reviewer_ids_map.values():
        all_team_ids.update(ids)
    teams_map = repo.batch_load_teams(db, list(all_team_ids))

    # User 일괄 조회 → UserSummary 매핑
    all_user_ids: set[uuid.UUID] = set()
    for issue in issues:
        if issue.created_by:
            all_user_ids.add(issue.created_by)
    for ids in assignee_ids_map.values():
        all_user_ids.update(ids)
    for reviewers in reviewer_details_map.values():
        for r in reviewers:
            all_user_ids.add(r.user_id)
    users = user_repo.get_users_by_ids(db, list(all_user_ids))
    user_summary_map: dict[uuid.UUID, UserSummary] = {
        u.id: user_mapper.to_user_summary(u) for u in users
    }

    result: dict[uuid.UUID, IssueEnrichment] = {}
    for issue in issues:
        issue_labels = [
            LabelBadge(id=lb.id, name=lb.name, color=lb.color)
            for lb in labels_map.get(issue.id, [])
        ]
        issue_assignees = [
            user_summary_map[uid]
            for uid in assignee_ids_map.get(issue.id, [])
            if uid in user_summary_map
        ]
        issue_assigned_teams = [
            TeamBadge(id=t.id, name=t.name)
            for tid in team_assignee_ids_map.get(issue.id, [])
            if (t := teams_map.get(tid))
        ]
        issue_reviewers = _build_reviewer_summaries(
            reviewer_details_map.get(issue.id, []),
            user_summary_map,
        )
        issue_reviewer_teams = [
            TeamBadge(id=t.id, name=t.name)
            for tid in team_reviewer_ids_map.get(issue.id, [])
            if (t := teams_map.get(tid))
        ]
        issue_parts = [
            PartBadge(id=p.id, part_number=p.part_number, name=p.name)
            for p in parts_map.get(issue.id, [])
        ]
        issue_files = [
            to_file_item(f) for f in files_map.get(issue.id, [])
        ]

        issue_linked_issues = [
            LinkedIssueBadge(
                id=li.id, number=li.number, title=li.title, state=li.state.value,
            )
            for li in linked_issues_map.get(issue.id, [])
        ]
        issue_linked_changes = [
            LinkedChangeRequestBadge(
                id=lc.id, number=lc.number, title=lc.title,
                state=lc.state.value, cr_state=lc.cr_state.value,
            )
            for lc in linked_crs_map.get(issue.id, [])
        ]

        result[issue.id] = IssueEnrichment(
            created_by=user_summary_map.get(issue.created_by) if issue.created_by else None,
            labels=issue_labels,
            assignees=issue_assignees,
            assigned_teams=issue_assigned_teams,
            reviewers=issue_reviewers,
            reviewer_teams=issue_reviewer_teams,
            parts=issue_parts,
            files=issue_files,
            comments_count=comment_counts.get(issue.id, 0),
            linked_issues=issue_linked_issues,
            linked_changes=issue_linked_changes,
        )

    return result
