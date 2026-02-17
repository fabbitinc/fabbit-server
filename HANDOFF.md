# HANDOFF — Dashboard Stats API 구현

## Goal

프론트엔드 대시보드 화면에 표시할 통계 API(`GET /api/v1/dashboard/stats`)를 구현한다. Part 총 개수, 이번 주 추가된 Part 수, 최근 합성 작업 정보를 반환하며, `total=0`이면 프론트에서 Empty State → Part 등록 유도 CTA를 보여줄 예정.

## Current Progress

- [x] Dashboard 모듈 신규 생성 (`app/modules/dashboard/`)
- [x] 스키마, 리포지토리, 서비스, 라우터 구현
- [x] `app/main.py`에 라우터 등록
- [x] 린트 통과 (새 파일), 전체 테스트 71 passed, 6 skipped

## 완료된 작업

### 신규 파일 (6개)

| 파일 | 내용 |
|---|---|
| `app/modules/dashboard/__init__.py` | 빈 패키지 |
| `app/modules/dashboard/schemas.py` | `PartStats`, `BomStats`, `LastSynthesis`, `DashboardStatsResponse` |
| `app/modules/dashboard/repository.py` | `count_parts`, `count_parts_since`, `count_bom_links`, `get_last_synthesis_job` |
| `app/modules/dashboard/service.py` | `get_stats()` — `@transactional(read_only=True)`, 최근 7일 기준 |
| `app/api/v1/tenant/dashboard_router.py` | `GET /api/v1/dashboard/stats` (인증 필수) |

### 수정 파일 (1개)

| 파일 | 변경 |
|---|---|
| `app/main.py` | `dashboard_router` import + `app.include_router(dashboard_router)` 추가 |

### 응답 구조

```json
{
  "parts": { "total": 42, "added_this_week": 5 },
  "bom_links": { "total": 120 },
  "last_synthesis": {
    "job_id": "uuid",
    "status": "COMPLETED",
    "completed_at": "2026-02-16T...",
    "nodes_created": 15,
    "relationships_created": 30
  }
}
```

- `last_synthesis`는 합성 이력이 없으면 `null`
- `added_this_week`는 `datetime.now(UTC) - timedelta(days=7)` 기준

## What Worked

- 기존 Part/BomLink/SynthesisJob 모델을 직접 import하여 재사용 — 새 모델 불필요
- `@transactional(read_only=True)` 패턴으로 읽기 전용 트랜잭션
- `get_last_synthesis_job`에서 `completed_at.desc().nullslast()` 정렬로 미완료 작업 후순위

## What Didn't Work / 주의사항

- 특별한 이슈 없음. 기존 패턴(Service-Repository, `get_tenant_db` Depends)을 그대로 따름

## 이전 컨텍스트 (BOM 업로드 설계 v2)

이전 세션에서 BOM 매핑/합성 파이프라인 v2 재설계가 완료됨:
- Part 속성 / 외부 관계 이분법, 5-phase 청크 처리
- 상세 내용은 git log 참고

## Next Steps

1. **커밋** — 현재 변경사항 커밋 (dashboard stats API)
2. **E2E 검증** — 서버 실행 후 실제 API 호출 테스트 (`curl` 또는 Swagger UI)
3. **단위 테스트** — dashboard service/repository 단위 테스트 추가 (선택)
4. **프론트엔드 연동** — `GET /api/v1/dashboard/stats` 호출하여 대시보드 화면 구성
5. **BOM v2 E2E 검증** — 3가지 BOM 유형 샘플 파일로 매핑 → 합성 → 조회 흐름 검증
