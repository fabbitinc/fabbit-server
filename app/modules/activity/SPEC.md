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
| `state_changed` | `{from, to}` | Issue open/close |
| `cr_state_changed` | `{from, to}` | CR 상태 전이 |
| `assignee_added` | `{user_ids}` | 담당자 배정 |
| `assignee_removed` | `{user_ids}` | 담당자 제거 |
| `labels_changed` | `{added: [{label_id, name, color}], removed: [{label_id, name, color}]}` | 라벨 동기화 |
| `part_added` | `{part_ids}` | 부품 연결 |
| `part_removed` | `{part_ids}` | 부품 해제 |
| `issue_linked` | `{linked_issue_ids}` | CR에 이슈 연결 |
| `issue_unlinked` | `{unlinked_issue_ids}` | CR에서 이슈 해제 |
| `title_changed` | `{old, new}` | 제목 수정 |

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
| IssueClosed | `state_changed` | `issue_closed` |
| IssueReopened | `state_changed` | `issue_reopened` |
| CRMerged | `cr_state_changed` | `cr_merged` |

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
