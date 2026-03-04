# FastAPI -> Spring Boot 마이그레이션 요약 (2026-03-04)

## 1. 수행 내용

- `../server` FastAPI의 전체 API(`152`개)를 Spring Boot에 경로/메서드 기준으로 전부 매핑 완료
- 기존 구현(Auth + Health) 유지하면서 누락 엔드포인트를 추가 이관
- 아키텍처 규칙(`Controller -> UseCase/Query`)을 지키고, 명령 흐름은 `UseCase -> Service -> Repository`, 조회 흐름은 `Query -> Repository`로 연결
- Swagger 어노테이션(`tag`, `summary`, `description`)을 컨트롤러에 반영

## 2. 주요 코드 변경

- 도메인별 API 계층 생성
  - `presentation/<domain>/controller`
  - `application/<domain>/query`
  - `application/<domain>/usecase`
  - `application/<domain>/service`
  - `domain/<domain>/repository`
- 공통 페이로드/저장소 지원 추가
  - `presentation/common/controller/ApiPayloadSupport`
  - `domain/common/repository/EndpointRepositorySupport`
- Auth 확장
  - `POST /api/v1/auth/refresh`
  - `POST /api/v1/auth/logout`
  - `GET /api/v1/auth/invitations/verify`
  - `POST /api/v1/auth/accept-invitation`
- Refresh 토큰 처리 강화
  - `JwtTokenService`에 refresh 검증/회전/폐기 로직 추가
  - `refresh_tokens` 영속화 연동
  - `RefreshRequest`, `RefreshTokenUseCase`, `LogoutUseCase` 추가
- 문서/의존성
  - `PLAN.md` 진행 현황/남은 논의 포인트 업데이트
  - Swagger annotation 의존성 추가 (`swagger-annotations-jakarta`)

## 3. 검증 결과

- 매핑 검증:
  - FastAPI: `152` routes
  - Spring: `153` routes (`/health` 1개 추가)
  - FastAPI 기준 누락: `0`
- 빌드/테스트:
  - `./gradlew test` 성공

## 4. 논의할 점

- Schema-per-Tenant 최종 방식 확정 필요
  - tenant 식별 기준(도메인/헤더/JWT)
  - `search_path` 적용 계층(Filter/Interceptor/DataSource proxy)
  - 커넥션 풀 재사용 시 tenant 컨텍스트 누수 방지
- 현재 비-Auth 도메인은 API 계약/레이어 구조를 우선 완성했으므로, 다음 단계에서 도메인 규칙/검증/영속 로직을 순차 정교화 필요
