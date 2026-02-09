-- Apache AGE 확장 설치 및 그래프 생성
CREATE EXTENSION IF NOT EXISTS age;
LOAD 'age';
SET search_path = ag_catalog, "$user", public;
SELECT create_graph('fabbit_graph');

-- column_mappings 테이블은 Alembic 마이그레이션으로 관리됩니다.
