# Playwright API 통합테스트

`playwright/`는 Fabbit 서버 OpenAPI 기반 API 통합테스트(TypeScript) 워크스페이스입니다.

## 범위

- OpenAPI 엔드포인트 전체 기준
  - 전체: `153`
  - 제외: `4`
  - 테스트 대상: `149`
- 제외 목록
  - `POST /api/v1/mappings/preview`
  - `POST /api/v1/activation/health-check`
  - `POST /api/v1/activation/query`
  - `GET /api/v1/activation/starters`

## 서버 준비

1. 서버 초기 실행

```bash
make dev-reset
```

2. 재실행

- 포그라운드 실행 중이면 종료 후 재실행
- 백그라운드 실행 중이면 `8000` 포트 점유 프로세스를 정리한 뒤 재실행

```bash
make dev-reset
```

## 설치

```bash
cd playwright
npm install
npm run generate:matrix
npm run check:coverage
```

## 실행

```bash
# 전체
npm test

# 계약 테스트(149개 대상 자동 생성)
npm run test:contracts

# 핵심 플로우
npm run test:flows

# 보안/격리
npm run test:security

# 멱등성
npm run test:idempotency

# 실패/재처리
npm run test:failure

# 스트레스
npm run test:stress

# 제외 4개 API 선택 실행 (기본 미실행)
npm run test:excluded
```

`test:excluded`는 기본 실행에 포함되지 않으며, 별도 실행 시에만 동작합니다.

## fixture

- CSV: `fixtures/csv/scope_stress_bom.csv`
- 매핑: `fixtures/mappings/scope_stress_bom.mapping.json`
- `confirm_mapping`은 위 고정 매핑을 사용하여 LLM 호출 없이 진행됩니다.

## 환경 변수

- `API_BASE_URL` (기본값: `http://127.0.0.1:8000`)
- 스트레스 튜닝
  - `PW_STRESS_READ_TOTAL`
  - `PW_STRESS_READ_CONCURRENCY`
  - `PW_STRESS_WRITE_TOTAL`
