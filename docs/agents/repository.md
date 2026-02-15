# Repository 작성 규칙

## 역할

- 데이터 접근 캡슐화 — service는 저장 위치(RDS/Graph)를 모름
- 모듈 함수로 작성, 첫 번째 인자: `db: Session`

## RDS-Graph 듀얼라이트

관계 데이터가 RDS와 Graph 양쪽에 필요한 경우 **단일 repository 함수에서 동시 쓰기**:

- **RDS**: 전체 속성 저장 (조회/필터링/정합성 보장)
- **Graph**: merge key만 유지 (관계 탐색/시각화용)
- 적용 대상: Part upsert, BOM 관계 (`CONSISTS_OF`), Project-Part 연결
- service는 듀얼라이트 여부를 알 필요 없음 — repository가 내부적으로 관리

## AGE Cypher 쿼리

- 동적 그래프 이름 필수: `cypher('{graph_name}', $$ ... $$)`
- `exec_driver_sql` 사용 시 `%` → `%%` 이스케이프 필수
- LLM 생성 Cypher는 세미콜론 제거: `query.strip().rstrip(";")`
- `apache-age-python` 패키지 사용 금지 — `database.py`의 connect 이벤트로 AGE 초기화
