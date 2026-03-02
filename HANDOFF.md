# Handoff

## Issue/CR 프로젝트 릴레이션 제거

### 변경 요약

Issue와 ChangeRequest에서 Project 종속성을 전면 제거했습니다.

- `issues.project_id` FK 컬럼 제거
- 번호 채번: `Project.issue_counter` → `pg_advisory_xact_lock(1)` + `MAX(number)+1` (테넌트 전역)
- API 경로: `/api/v1/projects/{project_id}/issues` → `/api/v1/issues`
- API 경로: `/api/v1/projects/{project_id}/changes` → `/api/v1/changes`
- `ProjectDetailResponse`에서 `open_issue_count`, `open_change_request_count` 필드 제거

### 제거된 프로젝트 피드 Activity Action 목록

아래 Action들은 Project 피드 전용이었으며, 이벤트 핸들러에서 제거되었습니다.
Action enum 값 자체는 DB 호환성을 위해 `constants.py`에 유지됩니다.

- `ISSUE_CREATED` — 이슈 생성 시 Project 피드
- `CR_CREATED` — 변경 요청 생성 시 Project 피드
- `ISSUE_CLOSED` — 이슈 닫기 시 Project 피드
- `ISSUE_REOPENED` — 이슈 재개 시 Project 피드
- `CR_MERGED` — CR 반영 시 Project 피드

향후 전역 Activity 설계 시 참조하세요.

### 마이그레이션 필요 사항 (미적용)

- `issues` 테이블: `project_id` 컬럼 DROP, `uq_issues_project_id_number` → `uq_issues_number` 변경, `ix_issues_project_id` 인덱스 DROP
- `projects` 테이블: `issue_counter` 컬럼 DROP
