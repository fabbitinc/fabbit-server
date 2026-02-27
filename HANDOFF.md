# Activity 모듈 구현 Handoff

## Goal

GitHub 이슈 타임라인처럼 이슈 상세 페이지에서 댓글+활동을 시간순으로 인터리빙하고, 프로젝트 페이지에서 상위 이벤트 피드를 보여주는 Activity 모듈 구현.

- 감사 로그가 아닌 **UI 피드** 목적
- 스펙: `app/modules/activity/SPEC.md`

## Current Progress

**구현 완료 — 마이그레이션 미적용 상태.**

### 새로 생성된 파일

| 파일 | 역할 |
|------|------|
| `app/modules/activity/__init__.py` | 모듈 패키지 |
| `app/modules/activity/constants.py` | `TargetType` Enum (PROJECT, ISSUE) |
| `app/modules/activity/models.py` | `Activity` 모델 (TimestampMixin + PkMixin, append-only) |
| `app/modules/activity/repository.py` | `add`, `list_by_target`, `list_by_target_cursor` |
| `app/modules/activity/schemas.py` | 응답 스키마 + 타임라인 스키마 |
| `app/modules/activity/mapper.py` | 모델→응답 변환 |
| `app/modules/activity/handlers.py` | 9개 이벤트 구독, Activity 레코드 생성 |
| `app/modules/issue/events.py` | Issue 도메인 이벤트 7종 |
| `app/modules/project/events.py` | Project 도메인 이벤트 2종 |
| `app/queries/issue/__init__.py` | `get_timeline` re-export |
| `app/queries/issue/get_timeline.py` | 댓글+활동 merge 타임라인 조회 |
| `app/queries/project/get_activities.py` | 프로젝트 활동 피드 cursor 기반 조회 |

### 수정된 파일

| 파일 | 변경 내용 |
|------|-----------|
| `app/modules/issue/models.py` | `AggregateRoot` 추가, close/reopen/open_for_review/merge/close에 이벤트 등록 |
| `app/modules/project/models.py` | `AggregateRoot` 추가 |
| `app/modules/issue/service.py` | create에 `IssueCreated` 이벤트, assign/unassign/link/unlink 시그니처 변경(issue_id→issue 객체) + 이벤트 등록 |
| `app/modules/project/service.py` | link_parts/unlink_parts 시그니처 변경(project_id→project 객체) + 이벤트 등록 |
| `app/modules/issue/repository.py` | `list_comments_by_issue` 함수 추가 |
| `app/use_cases/issue/assign_users.py` | 서비스 호출 시그니처 반영 |
| `app/use_cases/issue/unassign_users.py` | 서비스 호출 시그니처 반영 |
| `app/use_cases/issue/link_parts.py` | 서비스 호출 시그니처 반영 |
| `app/use_cases/issue/unlink_parts.py` | 서비스 호출 시그니처 반영 |
| `app/use_cases/project/link_parts.py` | 서비스 호출 시그니처 반영 |
| `app/use_cases/project/unlink_parts.py` | 서비스 호출 시그니처 반영 |
| `app/core/event_registry.py` | `activity.handlers` import 추가 |
| `app/queries/project/__init__.py` | `get_activities` re-export 추가 |
| `app/api/v1/tenant/issue_router.py` | `GET .../timeline` 엔드포인트 추가 |
| `app/api/v1/tenant/project_router.py` | `GET .../activities` 엔드포인트 추가 |

### API 엔드포인트

- `GET /api/v1/projects/{project_id}/issues/{issue_id}/timeline` — 댓글+활동 시간순 merge
- `GET /api/v1/projects/{project_id}/activities?cursor=&limit=` — 프로젝트 활동 피드 (cursor 기반 무한스크롤)

### 서비스 시그니처 변경 요약

이벤트 등록을 위해 AggregateRoot 인스턴스가 필요하므로, 일부 서비스 함수의 시그니처가 `issue_id`/`project_id` → `issue`/`project` 객체로 변경됨. use_case에서 `get_or_raise`로 조회한 객체를 그대로 전달하는 패턴.

### 이벤트 → Activity 매핑

| DomainEvent | Issue 피드 activity | Project 피드 activity |
|-------------|:-------------------:|:---------------------:|
| IssueCreated | - | `issue_created` |
| IssueStateChanged(→CLOSED) | `state_changed` | `issue_closed` |
| IssueStateChanged(→OPEN) | `state_changed` | `issue_reopened` |
| CRStateChanged(→MERGED) | `cr_state_changed` | `cr_merged` |
| CRStateChanged(other) | `cr_state_changed` | - |
| AssigneesAdded | `assignee_added` | - |
| AssigneesRemoved | `assignee_removed` | - |
| IssuePartsLinked | `part_added` | - |
| IssuePartsUnlinked | `part_removed` | - |
| ProjectPartsLinked | - | `part_added` |
| ProjectPartsUnlinked | - | `part_removed` |

## What Worked

- 기존 `file/handlers.py` 패턴을 그대로 따라 핸들러 구현
- 서비스 시그니처를 issue/project 객체로 변경하여 AggregateRoot.register_event() 자연스럽게 호출
- ChangeRequest.close()에서 CRStateChanged 등록 후 super().close()로 IssueStateChanged도 자동 등록 (dual-write)
- 프로젝트 활동 피드는 cursor 기반(UUID v7의 시간순 정렬 활용, `id < cursor`)으로 무한스크롤 지원

## What Didn't Work

- 없음 (첫 구현에서 모든 import 검증 통과)

## Next Steps

1. **DB 마이그레이션**: `activities` 테이블 생성 + `activity_target_type` Enum 타입 생성 (사용자가 별도 지시 시)
2. **통합 테스트**: 서버 기동 후 이슈 생성 → activities 테이블 레코드 확인, 타임라인 API 호출 검증
3. **SPEC.md에 있지만 아직 미구현인 이벤트**: `label_added`, `label_removed`, `title_changed`, `project_updated` (라벨/제목 변경 기능이 구현되면 추가)
