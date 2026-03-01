"""Activity 도메인 API Pydantic 스키마."""

import uuid
from datetime import datetime
from typing import Any, Literal

from pydantic import BaseModel, ConfigDict, Field, model_serializer

from app.modules.user.schemas import UserSummary


# ── Activity Detail 스키마 (action별 타입 정의) ──


class ActivityLabelInfo(BaseModel):
    """Activity에 기록되는 라벨 정보."""

    label_id: str
    name: str
    color: str


# -- 상태 전이 (from 은 Python 예약어이므로 alias + model_serializer 사용) --


class StateChangedDetail(BaseModel):
    """이슈 상태 변경 (OPEN ↔ CLOSED)."""

    model_config = ConfigDict(populate_by_name=True)

    action: Literal["issue_state_changed"]
    from_: str = Field(alias="from", description="변경 전 상태")
    to: str = Field(description="변경 후 상태")

    @model_serializer
    def _serialize(self):
        return {"action": self.action, "from": self.from_, "to": self.to}


class CRStateChangedDetail(BaseModel):
    """CR 상태 변경 (DRAFT → OPEN → MERGED/CLOSED)."""

    model_config = ConfigDict(populate_by_name=True)

    action: Literal["cr_state_changed"]
    from_: str = Field(alias="from", description="변경 전 상태")
    to: str = Field(description="변경 후 상태")

    @model_serializer
    def _serialize(self):
        return {"action": self.action, "from": self.from_, "to": self.to}


# -- 담당자 --


class AssigneesChangedDetail(BaseModel):
    """담당자 변경 (추가/제거)."""

    action: Literal["assignee_changed"]
    added: list[str]
    removed: list[str]


# -- 검토자 --


class ReviewersChangedDetail(BaseModel):
    """검토자 변경 (추가/제거)."""

    action: Literal["reviewer_changed"]
    added: list[str]
    removed: list[str]


# -- 라벨 --


class LabelsChangedDetail(BaseModel):
    """라벨 변경 (추가/제거)."""

    action: Literal["label_changed"]
    added: list[ActivityLabelInfo]
    removed: list[ActivityLabelInfo]


# -- 부품 --


class PartsChangedDetail(BaseModel):
    """부품 변경 (추가/제거) — 이슈 스코프."""

    action: Literal["part_changed"]
    added: list[str]
    removed: list[str]


class ProjectUpdatedDetail(BaseModel):
    """프로젝트 정보 수정."""

    action: Literal["project_updated"]
    changes: dict[str, Any]


class PartAddedDetail(BaseModel):
    """부품 연결 — 프로젝트 스코프."""

    action: Literal["part_added"]
    part_ids: list[str]


class PartRemovedDetail(BaseModel):
    """부품 해제 — 프로젝트 스코프."""

    action: Literal["part_removed"]
    part_ids: list[str]


# -- 파일 첨부/분리 --


class FileAttachedDetail(BaseModel):
    """파일 첨부."""

    action: Literal["file_attached"]
    file_ids: list[str]


class FileDetachedDetail(BaseModel):
    """파일 분리."""

    action: Literal["file_detached"]
    file_id: str


# -- CR-이슈 연결 --


class IssueLinkedDetail(BaseModel):
    """CR에 이슈 연결."""

    action: Literal["cr_issue_linked"]
    linked_issue_ids: list[str]


class IssueUnlinkedDetail(BaseModel):
    """CR에서 이슈 해제."""

    action: Literal["cr_issue_unlinked"]
    unlinked_issue_ids: list[str]


# -- 프로젝트 스코프 --


class IssueCreatedDetail(BaseModel):
    """이슈/CR 생성."""

    action: Literal["issue_created"]
    issue_id: str
    number: int
    title: str
    type: str


class IssueClosedDetail(BaseModel):
    """이슈 닫힘."""

    action: Literal["issue_closed"]
    issue_id: str
    number: int
    title: str


class IssueReopenedDetail(BaseModel):
    """이슈 재오픈."""

    action: Literal["issue_reopened"]
    issue_id: str
    number: int
    title: str


class CRMergedDetail(BaseModel):
    """CR 머지."""

    action: Literal["cr_merged"]
    issue_id: str
    number: int
    title: str


# -- Detail 유니온 (typed 스키마 → dict fallback 순서 중요) --
# Pydantic smart union이 action Literal로 자연 분기, 미매칭 시 dict 폴백.

IssueActivityDetail = (
    StateChangedDetail
    | CRStateChangedDetail
    | AssigneesChangedDetail
    | ReviewersChangedDetail
    | LabelsChangedDetail
    | PartsChangedDetail
    | FileAttachedDetail
    | FileDetachedDetail
    | IssueLinkedDetail
    | IssueUnlinkedDetail
    | dict[str, Any]
)

ProjectActivityDetail = (
    IssueCreatedDetail
    | IssueClosedDetail
    | IssueReopenedDetail
    | CRMergedDetail
    | PartAddedDetail
    | PartRemovedDetail
    | ProjectUpdatedDetail
    | dict[str, Any]
)


# ── Activity 응답 ──


class ActivityResponse(BaseModel):
    id: uuid.UUID
    action: str
    scope: str | None = None
    actor_id: uuid.UUID
    detail: ProjectActivityDetail | None = None
    created_at: datetime


class ActivityListResponse(BaseModel):
    items: list[ActivityResponse]
    next_cursor: uuid.UUID | None = None
    users: dict[str, UserSummary] = {}


# ── 타임라인 ──


class TimelineCommentItem(BaseModel):
    type: Literal["comment"] = "comment"
    id: uuid.UUID
    body: dict | None = None
    author_id: uuid.UUID | None = None
    created_at: datetime


class TimelineActivityItem(BaseModel):
    type: Literal["activity"] = "activity"
    id: uuid.UUID
    action: str
    scope: str | None = None
    actor_id: uuid.UUID
    detail: IssueActivityDetail | None = None
    created_at: datetime


class TimelineResponse(BaseModel):
    items: list[TimelineCommentItem | TimelineActivityItem]
    users: dict[str, UserSummary] = {}
