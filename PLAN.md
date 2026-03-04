# Server(FastAPI) -> Server2(Spring Boot) 전환 계획

## 1. 의사결정 요약

- 전환 방식: **Big Bang**
- 전략: **먼저 동일 동작 이관**, 구조 정리는 컷오버 직후 별도 스프린트에서 수행
- 이관 중 허용 변경: 보안/데이터 정합성/빌드 불가 같은 차단 이슈만 즉시 수정
- 통합테스트: 별도 트랙에서 작성 중이므로 본 계획 범위에서 제외

## 2. 목표 아키텍처(강제)

- `EventHandler -> UseCase -> Service -> Repository`
- `Controller -> UseCase -> Service -> Repository`
- `Controller -> Query -> Querydsl/Repository`

### 금지 규칙

- `Controller -> Service/Repository` 직접 호출 금지
- `EventHandler -> Service/Repository` 직접 호출 금지
- `Query -> UseCase/Service` 호출 금지
- `Repository -> Controller/UseCase/Query` 역참조 금지
- `UseCase -> Query` 호출 금지 (쓰기 흐름과 읽기 흐름 분리)

## 3. 추가 구조 원칙(확정)

### 3.1 트랜잭션 경계 고정

- 원칙: `@Transactional` 시작 지점은 **UseCase만 허용**
- Service/Repository/Query는 트랜잭션 시작 금지
- 읽기 Query는 기본적으로 read-only 경계 적용

### 3.2 Command/Query DTO 분리

- Controller Request/Response DTO와 UseCase Command/Result를 분리
- Query 전용 Response DTO를 별도로 관리
- API 스펙 DTO와 내부 처리 DTO를 같은 클래스로 재사용하지 않음

### 3.3 도메인 모델(Entity) 외부 노출 금지

- Controller 응답에 JPA Entity 직접 반환 금지
- Mapper 계층으로 Response DTO 변환 강제
- 엔티티 변경이 API 계약 변경으로 전파되지 않도록 차단

### 3.4 외부 연동만 선택적 Port 추상화

- 과한 헥사고날은 적용하지 않음
- 아래 외부 시스템만 Port/Adapter 적용:
  - LLM(Spring AI)
  - S3 호환 스토리지
  - 메일 발송
  - Apache AGE/Cypher 실행
- 내부 DB 접근(JPA Repository)은 직접 사용
- 내부 CRUD 전부를 Port/Adapter로 추상화하지 않음

### 3.5 이벤트 핸들러 운영 규칙

- EventHandler는 UseCase만 호출
- 이벤트 처리 로직은 멱등성 기준으로 작성
- 재처리/중복 처리 시 부작용이 없도록 상태 전이 가드 적용

### 3.6 아키텍처 룰 자동 검증

- 빌드 파이프라인에 아키텍처 검증 단계 추가
- 검증 항목:
  - 레이어 위반(import/호출)
  - 금지 의존 방향
  - Entity 직접 노출 패턴
- 권장 도구: ArchUnit(아키텍처 규칙), 정적 분석 규칙(Checkstyle/SpotBugs 등)

### 3.7 JPA 중심 DDD 모델 강화

- FastAPI의 부분적 ORM/DDD 적용 상태를 그대로 옮기지 않고, Spring에서는 JPA 모델링을 강하게 적용
- 도메인별 Aggregate Root를 명시하고, 외부 참조는 Root 기준으로만 접근
- Aggregate 경계 밖 연관은 ID 참조 우선, 무분별한 양방향 연관 금지
- 연관관계 기본 원칙:
  - `@ManyToOne(fetch = LAZY)` 기본
  - 컬렉션은 필요한 경우에만 `@OneToMany` 사용
  - `cascade = ALL`/`orphanRemoval = true`는 Aggregate 내부에서만 허용
- 엔티티는 setter 기반 빈약 모델 금지, 상태 전이 메서드로 비즈니스 규칙 캡슐화
- Repository는 Aggregate 단위 저장/조회에 집중하고, 화면 조회성 조인은 Query(Querydsl)로 분리
- N+1 방지를 위해 fetch join/EntityGraph/배치 전략을 Query 계층에서 명시적으로 관리
- DB 제약(Unique/FK/Index)과 도메인 불변조건을 함께 설계하여 정합성을 이중 보장

### 3.8 생성자/DI 패턴 표준화

- Spring Bean(`Controller`, `UseCase`, `Service`, `Query`, `EventHandler`)은 기본적으로 `@RequiredArgsConstructor` 사용
- 필드 주입(`@Autowired field`) 금지
- 수동 생성자는 특별한 이유(검증/정규화/다중 생성 정책)가 있을 때만 허용
- JPA Entity는 `protected` 기본 생성자 + 도메인 생성자/상태 전이 메서드 유지

### 3.9 인터페이스 도입 기준(YAGNI)

- 내부 비즈니스 계층(`UseCase/Service/Query`)은 단일 구현이면 인터페이스를 두지 않음
- 아래 경우에만 인터페이스 도입:
  - 다중 구현체가 실제로 필요함
  - 외부 시스템 포트(LLM/S3/Mail/AGE)로 교체 가능성이 높음
  - 모듈 경계 계약을 기술적으로 고정해야 함
- Spring Data JPA Repository는 예외로 유지(어댑터 추가 강제 없음)

### 3.10 에러코드 관리 표준(enum)

- 문자열 상수 대신 `ErrorCode enum`을 단일 기준으로 사용
- `ErrorCode`는 최소 `httpStatus`, `defaultMessage`를 포함
- `AppException`, `ErrorCode`는 `application` 레이어에 둔다
- `domain`은 HTTP/에러 응답 계약을 직접 참조하지 않는다
- API 응답은 `code`를 계약 키로 사용하고, `message`는 변경 가능 필드로 운영
- 도메인 확장 시 prefix 규칙 적용(예: `AUTH_*`, `ORG_*`, `MAPPING_*`)
- 전역 예외 처리기는 enum 기반으로 상태 코드/응답 코드를 일관 변환

### 3.11 UUID 정책 (v7 선할당)

- 엔티티 ID는 DB 생성이 아니라 애플리케이션 생성 시점에 할당
- UUID 버전은 `v7`을 표준으로 사용
- `persist` 이전에도 ID가 필요한 도메인 로직/이벤트 처리 가능하도록 설계
- JPA의 기본 UUID 생성 전략(`@UuidGenerator` 기본값)에 의존하지 않음

### 3.12 엔티티 코드 스타일

- 엔티티 접근자는 Lombok `@Getter`만 사용
- `@Setter`, `@Data` 금지
- 상태 변경은 도메인 메서드(행위)로만 수행
- JPA 기본 생성자는 `protected`로 제한

### 3.13 Abstract Entity 계층

- 감사 컬럼 강제 대신 선택형 추상 엔티티를 사용
- 권장 베이스:
  - `AbstractIdEntity` (`id`)
  - `AbstractCreatedEntity` (`id`, `createdAt`)
  - `AbstractAuditableEntity` (`id`, `createdAt`, `updatedAt`)
- 엔티티별 필요 수준만 선택해 상속

### 3.14 패키지 구조 표준 (Layer First)

- 루트 레이어는 다음 4개로 고정:
  - `presentation`
  - `application`
  - `domain`
  - `infrastructure`
- 의존 방향:
  - `presentation -> application`
  - `application -> domain`
  - `infrastructure -> domain, application`
  - `domain -> (none)`
- 외부 연동 포트는 `domain/application`, 구현은 `infrastructure`에 둔다
- JPA Repository는 도메인 패키지 인터페이스를 직접 사용한다

### 3.15 Swagger(OpenAPI) 문서화 규칙

- 모든 API는 Swagger(OpenAPI) 문서를 기본 제공한다
- Controller/엔드포인트에 `tag`, `summary`, `description`을 명시한다
- 요청/응답 DTO 필드에는 의미가 드러나도록 `description`을 작성한다
- 주요 에러 응답 코드와 의미를 문서에 함께 표기한다
- 프론트/외부 연동에서 바로 이해 가능하도록 예시값(example)도 가능한 범위에서 제공한다

## 4. 패키지/모듈 구조 초안

```text
com.fabbitinc.server
  ├─ presentation
  │   ├─ common
  │   └─ <domain>/controller
  ├─ application
  │   ├─ config
  │   └─ <domain>/(usecase|service|query|dto|eventhandler)
  ├─ domain
  │   ├─ common
  │   └─ <domain>/(model|repository|policy|event)
  └─ infrastructure
      ├─ persistence
      └─ external
```

## 5. 빅뱅 실행 단계

### Phase 0. 기준선 고정

- FastAPI OpenAPI를 계약 기준으로 고정
- 에러 코드/응답 포맷/권한 규칙 기준 문서화
- DB 스키마(public + tenant_*) 기준선 고정

### Phase 1. 부트스트랩/공통 기반

- Spring Boot 기본 구조 + 공통 예외/응답/인증 필터 구성
- 멀티테넌시(search_path)와 DB 연결 정책 구현
- Flyway/Liquibase 마이그레이션 체계 정립(public/tenant 트랙 분리)

### Phase 2. 외부 연동 어댑터 이관

- Spring AI 기반 LLM 어댑터
- S3/메일/AGE 어댑터
- 설정/시크릿/타임아웃/재시도 정책 반영

### Phase 3. 핵심 플로우 우선 이관

- `Auth -> File(Upload) -> Mapping -> Synthesis -> Activation` 순으로 이관
- 기존 FastAPI 동작/응답 호환 우선, 구조 개선 최소화
- 단, 엔티티/애그리거트 모델은 JPA 기준으로 강하게 정리하여 이관

### Phase 4. 협업 도메인 이관

- Organization/Member/User/Team/Project/Part/Issue/Notification/Usage 순차 이관
- 읽기 API는 Query 계층으로 분리하여 구현

### Phase 5. 컷오버 준비

- 운영 설정/배포 파이프라인/롤백 절차 확정
- 데이터 마이그레이션 및 스키마 정합성 점검
- 컷오버 체크리스트 리허설

### Phase 6. Big Bang 컷오버 및 안정화

- 트래픽 전환
- 장애 대응/핫픽스(차단 이슈 우선)
- 전환 직후 구조 정리 백로그 실행

## 6. 완료 기준(DoD)

- 모든 API가 목표 레이어 규칙 내에서 동작
- 핵심/협업 도메인 전부 Spring Boot로 이관 완료
- 아키텍처 룰 자동 검증에서 위반 0건
- 운영 컷오버 후 FastAPI 서비스 종료 가능 상태

## 7. 전환 중 작업 원칙

- 이관 중 리팩토링은 최소화
- 코드 스타일 통일/네이밍 정리는 컷오버 후 수행
- "동작 동일성"을 최우선으로 유지

## 8. 현재 진행 현황 (2026-03-04)

- FastAPI 기준 API `152`개를 Spring Boot로 전부 매핑 완료 (`/health`는 Spring 전용 추가 엔드포인트로 유지)
- 레이어 규칙 준수:
  - `Controller -> UseCase/Query`
  - `Query -> Repository`
  - `UseCase -> Service -> Repository`
- Auth 도메인:
  - `send-verification`, `verify-email`, `register`, `login`, `refresh`, `logout` 연결 완료
  - refresh 토큰은 DB(`refresh_tokens`) 저장/회전/폐기 방식으로 동작
- 비-Auth 도메인:
  - FastAPI 계약과 동일한 경로/HTTP 메서드로 전부 이관
  - 도메인별로 `Controller`, `UseCase`, `Service`, `Query`, `Repository` 코드 생성 완료
- Swagger 문서화:
  - Controller 단위 `tag` 적용
  - 엔드포인트 단위 `summary/description` 적용

## 9. 남은 논의/결정 포인트

- Schema-per-Tenant 최종 구현 상세:
  - 요청 단위 tenant 식별 전략(도메인/헤더/JWT 우선순위)
  - `search_path` 설정 위치(Filter/Interceptor/DataSource proxy)
  - 커넥션 풀 재사용 시 tenant 컨텍스트 누수 방지 정책
- 도메인별 실비즈니스 로직 정교화 우선순위:
  - `File -> Mapping -> Synthesis -> Activation` 우선
  - 이후 `Organization/Member/User/Team/Project/Part/Issue/Notification/Usage` 순으로 고도화
