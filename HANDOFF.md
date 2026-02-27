# Handoff Document

## Goal

제조업 PLM 서버에 Label 기능과 Project 생성 API를 구현한다.

- Project 단위 라벨(Label) CRUD
- Project 생성 시 PLM 기본 라벨 9종 자동 생성
- Project 생성 API (`POST /api/v1/projects`)

## Current Progress — 완료

커밋: `b39fc87 feat(label): Label 모듈 구현 및 Project 생성 API 추가`

### 신규 파일 (15개)

| 파일 | 역할 |
|------|------|
| `app/modules/label/__init__.py` | 모듈 패키지 |
| `app/modules/label/models.py` | `Label` ORM (AuditMixin + UpdatableMixin + PkMixin + TenantBase) |
| `app/modules/label/constants.py` | 기본 라벨 9종 (우선순위 3 + 유형 6) |
| `app/modules/label/schemas.py` | Create/Update 요청 + LabelResponse/LabelListResponse |
| `app/modules/label/repository.py` | get_by_id, get_by_project_and_name, list_by_project, add, add_all, delete |
| `app/modules/label/mapper.py` | Label → LabelResponse 변환 |
| `app/modules/label/service.py` | create_label, update_label, delete_label, seed_defaults |
| `app/use_cases/label/create_label.py` | 프로젝트 검증 → 라벨 생성 |
| `app/use_cases/label/update_label.py` | 라벨 수정 (PATCH, description null 해제 지원) |
| `app/use_cases/label/delete_label.py` | 라벨 삭제 |
| `app/queries/label/list_labels.py` | 프로젝트 라벨 목록 조회 |
| `app/api/v1/tenant/label_router.py` | GET/POST/PATCH/DELETE 4개 엔드포인트 |
| `app/use_cases/project/create_project.py` | 프로젝트 생성 + 기본 라벨 seed |

### 수정 파일 (6개)

| 파일 | 변경 내용 |
|------|-----------|
| `app/modules/project/repository.py` | `add()` 함수 추가 |
| `app/modules/project/service.py` | `create_project()` 함수 추가 |
| `app/modules/project/schemas.py` | `CreateProjectRequest` 스키마 추가 |
| `app/use_cases/project/__init__.py` | `create_project` re-export |
| `app/api/v1/tenant/project_router.py` | `POST /api/v1/projects` 엔드포인트 추가 |
| `app/main.py` | `label_router` 등록 |

### API 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/v1/projects` | 프로젝트 생성 (기본 라벨 자동 생성) |
| GET | `/api/v1/projects/{project_id}/labels` | 라벨 목록 |
| POST | `/api/v1/projects/{project_id}/labels` | 라벨 생성 |
| PATCH | `/api/v1/projects/{project_id}/labels/{label_id}` | 라벨 수정 |
| DELETE | `/api/v1/projects/{project_id}/labels/{label_id}` | 라벨 삭제 |

## What Worked

- 기존 Issue 모듈 패턴을 그대로 따라 일관된 구조 유지
- 프로젝트 아키텍처 스킬(`models-guide`, `api-guide`, `business-layer-guide`, `repository-guide`)을 사전 로드하여 컨벤션 준수
- `UpdateLabelRequest`에서 `model_dump(exclude_unset=True)`로 description null 명시 전달과 미전달을 구분

## What Didn't Work

- 특별히 실패한 접근 없음

## Next Steps

- **마이그레이션**: `labels` 테이블 Alembic 마이그레이션 생성 필요 (사용자 별도 지시 시)
- **Issue ↔ Label 연결**: Issue에 Label을 붙이는 M:N 관계 (`issue_labels` 테이블) 구현 필요
- **라벨 필터링**: Issue 목록 조회 시 라벨 기준 필터링
- **Project 수정/삭제 API**: 현재 생성만 있음
