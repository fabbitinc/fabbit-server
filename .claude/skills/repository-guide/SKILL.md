---
name: repository-guide
description: "Repository 작성 규칙. repository.py, 데이터 접근, RDS-Graph 듀얼라이트, AGE Cypher 쿼리를 작성하거나 수정할 때 자동 참조."
user-invocable: false
---

# Repository 작성 규칙

## 역할

- 데이터 접근 캡슐화 — service는 저장 위치(RDS/Graph)를 모름
- 모듈 함수로 작성, 첫 번째 인자: `db: Session`

## RDS-Graph 듀얼라이트

관계 데이터가 RDS와 Graph 양쪽에 필요한 경우 **단일 repository 함수에서 동시 쓰기**:

- **RDS**: 전체 속성 저장 (조회/필터링/정합성 보장)
- **Graph**: merge key만 유지 (관계 탐색/시각화용)
- 노드 적용 대상: Part, Drawing, Supplier upsert
- 관계 적용 대상: `CONSISTS_OF`, `HAS_ITEM`, `DEFINED_BY`, `SUPPLIED_BY`
- service는 듀얼라이트 여부를 알 필요 없음 — repository가 내부적으로 관리

## 예외 처리

- repository에서 `try/except` 금지 — 예외는 호출자(service)에게 그대로 전파
- 폴백(기본값 반환, 스킵 등) 결정은 비즈니스 로직이므로 service에서 처리
- `db.rollback()` 직접 호출 금지 — 트랜잭션 경계는 `@transactional`이 관리

## AGE Cypher 쿼리

- 동적 그래프 이름 필수: `cypher('{graph_name}', $$ ... $$)`
- `exec_driver_sql` 사용 시 `%` → `%%` 이스케이프 필수
- LLM 생성 Cypher는 세미콜론 제거: `query.strip().rstrip(";")`
- `apache-age-python` 패키지 사용 금지 — `database.py`의 connect 이벤트로 AGE 초기화
