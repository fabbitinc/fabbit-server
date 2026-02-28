# Activity 모듈 설계 스펙

## 개요

사용자에게 보여주는 활동 이력(타임라인). 감사 로그가 아니라 **UI 피드** 목적.

- 기록할 때부터 필요한 scope에만 기록 (프론트 필터링 X)
- 댓글은 Activity에 기록하지 않음 (comments 테이블에서 직접 조회)

## 모델

```
activities
├── id: UUID (PK)
├── target_type: Enum (PROJECT | ISSUE)
├── target_id: UUID
├── action: String
├── actor_id: UUID
├── detail: JSONB
├── created_at: DateTime
```

## Issue 타임라인 API

단일 엔드포인트에서 comments + activities를 `created_at` 기준으로 merge하여 반환.

```
GET /issues/{id}/timeline
→ [
    {type: "comment", id, body, author_id, created_at},
    {type: "activity", action: "label_added", actor_id, detail, created_at},
    ...
  ]
```

## 이벤트 기록 매핑

### Issue scope (target_type=ISSUE)

이슈 상세 페이지의 타임라인에 표시.

| action | detail | 트리거 |
|--------|--------|--------|
| `issue_state_changed` | `{from, to}` | Issue open/close |
| `cr_state_changed` | `{from, to}` | CR 상태 전이 |
| `assignee_added` | `{user_ids}` | 담당자 배정 |
| `assignee_removed` | `{user_ids}` | 담당자 제거 |
| `label_changed` | `{added: [{label_id, name, color}], removed: [{label_id, name, color}]}` | 라벨 동기화 |
| `part_added` | `{part_ids}` | 부품 연결 |
| `part_removed` | `{part_ids}` | 부품 해제 |
| `cr_issue_linked` | `{linked_issue_ids}` / `{cr_id, cr_number, cr_title}` | CR에 이슈 연결 (양방향) |
| `cr_issue_unlinked` | `{unlinked_issue_ids}` / `{cr_id, cr_number, cr_title}` | CR에서 이슈 해제 (양방향) |
| `issue_title_changed` | `{old, new}` | 제목 수정 |

### Project scope (target_type=PROJECT)

프로젝트 페이지의 활동 피드에 표시. **상위 이벤트만** 기록.

| action | detail | 트리거 |
|--------|--------|--------|
| `project_updated` | `{changed_fields}` | 프로젝트 정보 수정 |
| `part_added` | `{part_ids}` | 프로젝트에 부품 추가 |
| `part_removed` | `{part_ids}` | 프로젝트에서 부품 제거 |
| `issue_created` | `{issue_id, number, title, type}` | Issue/CR 생성 |
| `issue_closed` | `{issue_id, number, title}` | Issue 닫힘 |
| `issue_reopened` | `{issue_id, number, title}` | Issue 재오픈 |
| `cr_merged` | `{issue_id, number, title}` | CR 머지 |

### Dual-write 이벤트

아래 이벤트는 양쪽 scope 모두에 기록:

| DomainEvent | Issue 피드 | Project 피드 |
|-------------|:---------:|:-----------:|
| IssueClosed | `issue_state_changed` | `issue_closed` |
| IssueReopened | `issue_state_changed` | `issue_reopened` |
| CRMerged | `cr_state_changed` | `cr_merged` |

## Activity Scope

action을 도메인 영역별로 그룹화하여 프론트엔드에서 필터링할 수 있다.
DB 컬럼 없이 코드 레벨 매핑으로 관리 (action과 1:1).

| scope | actions |
|-------|---------|
| `issue` | `issue_state_changed`, `issue_title_changed`, `issue_created`, `issue_closed`, `issue_reopened` |
| `cr` | `cr_state_changed`, `cr_merged`, `cr_issue_linked`, `cr_issue_unlinked` |
| `part` | `part_added`, `part_removed` |
| `assignee` | `assignee_added`, `assignee_removed` |
| `label` | `label_changed` |
| `project` | `project_updated` |

API: `GET /projects/{id}/activities?scope=issue&scope=part` — 복수 scope 지정 가능.

## 데이터 흐름

```
Service (액션 수행)
  → AggregateRoot.register_event(DomainEvent)

UnitOfWork.commit()
  → EventBus.publish_all()

ActivityHandler (구독)
  → 이벤트별 매핑에 따라 Activity 1~2개 생성
  → 같은 트랜잭션 내 저장
```
