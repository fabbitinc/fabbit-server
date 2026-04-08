# Fabbit Server

B2B SaaS 제조업 PLM/MES 플랫폼의 백엔드입니다. Spring Boot 4와 Java 21로 구축했으며, 멀티테넌트 아키텍처 위에서 부품 라이프사이클, CAD 도면, BOM, 엔지니어링 변경 워크플로우 전체를 처리합니다.

> 실제 프로덕션 출시를 목표로 개발된 프로젝트이며, 현재는 포트폴리오 레퍼런스로 공개합니다.

---

## 기술 스택

| 분류 | 기술 |
|---|---|
| 런타임 | Java 21, Spring Boot 4.x |
| 영속성 | PostgreSQL, Spring Data JPA, QueryDSL, Liquibase |
| 보안 | Spring Security, JWT (Auth0 java-jwt) |
| AI / LLM | Spring AI, OpenAI 호환 API |
| 파일 처리 | Apache PDFBox, Apache POI, TwelveMonkeys ImageIO |
| CAD 변환 | Mayo (C++, STEP/IGES), ezdxf (Python, DXF) |
| 테스트 | JUnit 5, Testcontainers, ArchUnit, Mockito |
| 빌드 | Gradle 8, Kotlin DSL, Spotless |
| 배포 | Docker (멀티스테이지), AWS S3, OCI |

---

## 아키텍처

엄격한 **4계층 헥사고날 아키텍처**입니다. ArchUnit 테스트는 AI가 코드를 생성할 때 아키텍처 규칙을 벗어나지 않도록 하는 가드레일(하네스 엔지니어링)이며, CI에서도 동일하게 검증합니다.

```
┌─────────────────────────────────────────────────┐
│                  Presentation                   │  HTTP / REST / SSE
│         Controllers  ·  DTOs  ·  Mappers        │
└────────────────────┬────────────────────────────┘
                     │ (Entity 타입은 이 경계를 넘을 수 없음)
┌────────────────────▼────────────────────────────┐
│                  Application                    │  유스케이스 · 서비스
│   UseCase  ·  Service  ·  Query  ·  EventHandler│
└──────────┬─────────────────────────┬────────────┘
           │                         │
┌──────────▼──────────┐  ┌───────────▼────────────┐
│       Domain        │  │    Infrastructure      │
│  Aggregates  ·      │  │  Persistence · Storage │
│  Value Objects  ·   │  │  AI  ·  Email  ·  CAD  │
│  Domain Events      │  │  멀티테넌시 설정         │
└─────────────────────┘  └────────────────────────┘
```

**레이어 의존 규칙 (ArchUnit 강제):**

- `Presentation` → `Application`, `Domain`만 참조 가능
- `Application` → `Domain`만 참조 가능
- `Infrastructure` → `Application`, `Domain` 참조 가능
- `Domain` → 외부 의존 없음 (완전 격리)

추가 규칙: 컨트롤러에서 JPA Entity 타입 노출 금지, 서비스 간 직접 호출 시 `*Api`/`*Policy` 인터페이스 경유 필수, 이벤트 핸들러에서 타 도메인 유스케이스 직접 호출 금지.

---

## 도메인 모델

제조 엔지니어링 라이프사이클을 커버하는 **28개 도메인 애그리게이트**입니다.

```
핵심 엔지니어링                    협업
─────────────────               ─────────────
Part  ←──────── Revision        Issue
  └── PartCategory              EngineeringChange  ──► ECR / ECN 워크플로우
  └── ExtendedProperty          Notification
                                Activity  (감사 로그)
제조
─────────────                   플랫폼
BOM  ←──── BomItem              Organization  (테넌트 루트)
Drawing ──► Artifact            Team  ·  Member
WorkItem                        User  ·  Auth
Project                         Subscription
                                AiUsage  (토큰 집계)
AI
────────────────
Chat  ←──── ChatConversation
  └── ChatMessage
  └── 툴 콜링 (PartLookup · IssueLookup · IssueCreateDraft)
```

모든 애그리게이트 루트는 `AbstractIdEntity<UUID>`를 상속하고 `AggregateRoot` 마커 인터페이스를 구현합니다. 퍼블릭 세터는 ArchUnit으로 금지되며, 상태 변경은 비즈니스 의도를 표현하는 명시적 도메인 메서드를 통해서만 가능합니다.

---

## 멀티테넌시

각 고객 조직은 독립적인 **PostgreSQL 스키마**에서 운영됩니다. 요청별로 스키마를 전환하여 테넌트 간 데이터 유출 가능성을 구조적으로 차단합니다.

```
요청
  │
  ▼
TenantContextFilter             JWT 클레임에서 조직 슬러그 추출
  │
  ▼
TenantContextHolder             ThreadLocal에 스키마명 저장
  │
  ▼
CurrentTenantSchemaIdentifierResolver   TenantContextHolder 읽기
  │
  ▼
SchemaBasedMultiTenantConnectionProvider
  └── SET search_path = '<tenant_schema>, ag_catalog, public'
```

테넌트 스키마는 새 조직 생성 시 `TenantProvisioningAdapter`가 동적으로 프로비저닝합니다. Liquibase는 `public` 스키마(사용자, 조직, 구독)와 테넌트 스키마(부품, 도면, BOM, 이슈)에 대해 별도 체인지로그를 실행합니다.

---

## CAD 처리 파이프라인

CAD 파일 처리는 세 가지 런타임을 조율해야 합니다. 서버는 파일 유형별로 단일 파이프라인으로 통합하여 처리합니다.

```
업로드
  │
  ├─► CAD 2D  (DXF, DWG)
  │     └── EzdxfCad2dToPdfAdapter          Python 서브프로세스 (ezdxf)
  │           └── PdfBoxPdfPreviewRender     Java → WebP 썸네일
  │
  ├─► CAD 3D  (STEP, IGES, OBJ)
  │     └── Mayo3dConverterAdapter           C++ 서브프로세스 (mayo-conv)
  │           └── GLB + 썸네일 생성
  │
  ├─► PDF
  │     └── PdfBoxPdfPreviewRender           Java → WebP 썸네일
  │
  └─► 래스터  (PNG, JPG, TIFF, WebP)
        └── PdfBoxRasterImageToPdfAdapter    Java → PDF
              └── ImageIoWebpTranscoder      TwelveMonkeys → WebP
```

각 파이프라인은 `DrawingPipelineDeadlineContext` 안에서 실행되며, 서브프로세스 호출에 하드 타임아웃을 적용하여 느린 CAD 변환이 스레드 풀을 점유하지 않도록 합니다.

Docker 이미지는 멀티스테이지 빌드에서 `mayo-conv`를 소스에서 컴파일하고 `ezdxf`를 pip으로 설치한 후 두 바이너리를 최종 JRE 런타임 이미지에 복사합니다.

---

## LLM 통합

`chat` 도메인은 SSE 기반 AI 어시스턴트를 제공합니다. 어시스턴트는 부품 조회, 관련 이슈 검색, 대화에서 직접 이슈 초안 생성이 가능합니다.

```
ChatController  (SSE 엔드포인트)
  │
  ▼
ExecuteChatActionUseCase
  │
  ▼
ChatAgentService
  ├── Spring AI ChatClient (OpenAI 호환 API)
  │     툴: PartLookupTool
  │          PartIssueLookupTool
  │          IssueCreateDraftTool
  │
  └── ChatSsePublisher  → 클라이언트로 토큰 스트리밍
```

토큰 사용량은 Micrometer `ObservationHandler`(`ChatUsageObservationHandler`)로 캡처되어 `AiUsageEvent` 도메인 객체로 영속화됩니다. 이를 통해 조직별 비용 추적과 예산 제한이 가능합니다.

LLM 엔드포인트는 설정으로 교체 가능합니다(기본값 OpenRouter).

---

## 이벤트 기반 도메인 간 통신

도메인은 Spring `ApplicationEvent` 객체를 통해 통신합니다. 애그리게이트 경계를 유지하면서 서비스 간 직접 결합을 방지합니다.

```java
// ReleaseEngineeringChangeUseCase에서 발행
record EngineeringChangeReleasedEvent(
    UUID engineeringChangeId,
    UUID actorId,
    String ecNumber,
    String ecTitle
) { }

// 별도 컴포넌트에서 구독
@EventListener
void handle(EngineeringChangeReleasedEvent event) {
    createReleaseNotificationsUseCase.execute(...);
}
```

주요 이벤트: `NotificationCreatedEvent`, `WorkItemUsersMentionedEvent`, `ChatRunEvent` 등.

---

## 보안

인증은 스테이트리스 JWT입니다. 각 토큰에 조직 컨텍스트가 포함되어 있어 테넌트 필터가 DB 추가 조회 없이 올바른 스키마를 결정할 수 있습니다.

```
Authorization: Bearer <token>
  │
  ▼
JwtSecurityContextFilter  (OncePerRequestFilter)
  ├── AuthTokenParser.requireAuth()
  ├── SecurityContext에 AuthPrincipal 주입
  │     { userId, email, orgId, role }
  └── 하위 코드는 SecurityContextCurrentAuthProvider로 읽음
```

`JwtProperties`는 시작 시 서명 시크릿이 최소 32바이트 이상인지 검증하고, 개발 환경에서 약한 기본값 사용 시 경고를 로깅합니다.

---

## 아키텍처 테스트

ArchUnit 테스트는 AI 코드 생성의 가드레일이자 CI 검증 수단으로, 표준 `./gradlew test`에 포함되어 위반 시 빌드를 실패시킵니다.

**레이어 규칙** (`LayerArchitectureRulesTest`):
- 컨트롤러에서 도메인 모델 패키지 참조 금지
- 컨트롤러 메서드 시그니처에 JPA Entity 타입 노출 금지
- 이벤트 핸들러에서 타 도메인 유스케이스 호출 금지
- 쿼리에서 유스케이스 또는 서비스 호출 금지
- 서비스의 도메인 간 호출은 `*Api` 또는 `*Policy` 인터페이스 경유 필수

**엔티티 규칙** (`EntityArchitectureRulesTest`):
- 모든 엔티티는 `AbstractIdEntity` 상속 필수
- 모든 엔티티에 protected 기본 생성자 선언 필수
- 퍼블릭 세터 금지
- 읽기 전용 관계 필드는 `_<name>Relation` 네이밍 + `insertable=false, updatable=false` 필수

---

## 로컬 실행

**사전 요구사항:** Docker, Java 21, Gradle 8

```bash
# 의존성 서비스 시작 (PostgreSQL, MinIO, Mailpit)
docker compose up -d

# dev 프로파일로 실행
./gradlew bootRun --args='--spring.profiles.active=dev'

# API 문서
open http://localhost:10010/swagger-ui.html
```

**테스트 실행:**

```bash
./gradlew test              # 단위 테스트 + ArchUnit
./gradlew integrationTest   # 통합 테스트 (Testcontainers)
./gradlew schemaExport      # JPA 모델에서 DDL 생성
```

---

## 프로젝트 구조

```
src/main/java/com/fabbitinc/server/
├── presentation/       REST 컨트롤러, 요청/응답 DTO
├── application/        유스케이스, 서비스, 쿼리, 이벤트 핸들러
├── domain/             애그리게이트, 값 객체, 도메인 이벤트
└── infrastructure/
    ├── persistence/    Hibernate 설정, 멀티테넌시, 레포지토리
    ├── security/       JWT 필터, Spring Security 설정
    ├── drawing/        CAD 변환 파이프라인 어댑터
    ├── external/       S3, SMTP, 결제, 인증 어댑터
    └── scheduling/     비동기 실행기, 스케줄 작업
```

---

## 주요 설계 결정

**스키마 기반 멀티테넌시를 선택한 이유**  
PostgreSQL `search_path` 전환은 완전한 스키마 격리를 제공하며, `WHERE tenant_id = ?` 누락으로 인한 데이터 유출 위험이 없습니다. 테넌트별 백업 및 복원도 단순화됩니다.

**ArchUnit으로 아키텍처를 강제하는 이유**  
AI 어시스턴트와 협업하여 코드를 생성할 때, AI가 아키텍처 규칙을 벗어나는 코드를 만들 수 있습니다. ArchUnit은 이런 위반을 자동으로 감지하는 가드레일(하네스 엔지니어링)이며, CI에서도 동일하게 검증하여 아키텍처 일관성을 보장합니다.

**UseCase와 Query를 분리한 이유**  
유스케이스는 트랜잭션과 쓰기 작업을 소유합니다. 쿼리는 `@Transactional(readOnly=true)`로 표시되며 쓰기를 트리거하지 않습니다. 이 분리로 클래스가 상태를 변경하는지 즉시 파악할 수 있고, 읽기 전용 트랜잭션은 읽기 중심 경로에서 더 나은 성능을 발휘합니다.

**순수 Java 라이브러리 대신 서브프로세스 기반 CAD 변환을 선택한 이유**  
CAD 산업의 사실상 표준은 OpenCascade(C++)입니다. Mayo는 STEP/IGES 처리를 위해 OpenCascade 위에 구축된 최고의 오픈소스 컨버터이며, ezdxf는 가장 완성도 높은 DXF 파서입니다. 이를 Java로 재작성하는 것은 수년이 걸리는 작업입니다. 서브프로세스 모델은 Java 서비스를 스테이트리스로 유지하면서 각 컨버터를 독립적으로 버전 관리할 수 있게 합니다.
