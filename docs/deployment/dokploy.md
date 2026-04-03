# Dokploy 배포 가이드

## 목표

- 운영 배포 브랜치는 `release` 하나만 사용한다.
- Dokploy는 GitHub 저장소를 직접 감시한다.
- `release` 브랜치 변경만 자동 배포한다.
- 배포 시 CLI를 호출하지 않는다.
- Dokploy CLI는 최초 프로젝트/앱 bootstrap 용도로만 선택적으로 사용한다.

## 현재 저장소 기준 배포 전제

- 애플리케이션은 Spring Boot Java 21이다.
- 기본 포트는 `10010` 이다.
- 운영 프로필은 `SPRING_PROFILES_ACTIVE=prod` 로 구동한다.
- 헬스체크 엔드포인트는 `GET /health` 이다.

## 권장 Git Workflow

1. `feature/*` -> `main` 으로 PR
2. 운영 배포 시점에 `main` -> `release` PR 생성
3. `release` 머지 후 GitHub에 push 발생
4. Dokploy가 `release` 브랜치 변화를 감지하고 자동 배포
5. hotfix는 `release` 에서 분기해서 `release` 로 머지한 뒤, 반드시 `main` 으로 역머지

## 1회성 Dokploy bootstrap

Dokploy 프로젝트/앱이 아직 없으면 아래 스크립트로 생성한다.

```bash
./scripts/dokploy/bootstrap-project.sh project
```

프로젝트 생성 후 **앱 생성은 Dokploy UI에서 진행한다.**

```bash
./scripts/dokploy/bootstrap-project.sh project
```

> 전제: 로컬에서 `dokploy authenticate` 가 이미 완료되어 있어야 한다.
> 참고: 2026-04-02 기준 Dokploy CLI v0.2.8 의 `app create` 는 서버가 요구하는
> `environmentId` 를 보내지 않아 400 BAD_REQUEST가 발생했다. 따라서 앱 생성은 UI 기준으로 진행한다.

## Dokploy UI 설정

앱 생성 후 Dokploy UI에서 아래를 설정한다.

### General / Source

- Git provider: `GitHub`
- Repository: `fabbitinc/fabbit-server`
- Branch: `release`
- Auto deploy: `enabled`

### Runtime

- Port: `10010`
- Start profile env: `SPRING_PROFILES_ACTIVE=prod`

### Health check

- Path: `/health`
- URL example: `http://localhost:10010/health`

### Optional Watch Paths

문서 변경만으로 재배포되는 것을 막고 싶으면 watch paths를 아래처럼 제한한다.

```text
src/**
build.gradle.kts
settings.gradle.kts
gradle/**
Dockerfile
```

## Dokploy Compose 설정

Apache AGE DB는 Dokploy **Docker Compose** 서비스로 분리하는 것을 권장한다.

### Compose file path

- `docker/docker-compose.dokploy.yml`

### Compose 환경 변수

Dokploy Compose 서비스에는 최소 아래 값을 넣는다.

```dotenv
POSTGRES_USER=fabbit
POSTGRES_PASSWORD=<strong-password>
POSTGRES_DB=fabbit
```

### Compose 리소스 권장값

- CPU: `0.75`
- Memory reservation: `1g`
- Memory limit: `3g`

### Compose 서비스 역할

- `fabbit-database`: Apache AGE PostgreSQL
- 이미지 태그는 `apache/age:release_PG18_1.7.0` 로 고정
- PostgreSQL 18 계열 컨테이너 규칙에 맞춰 데이터 볼륨은 `/var/lib/postgresql` 에 마운트
- `docker/initdb.d/01_init_age.sql` 로 AGE extension + graph 초기화
- DB 포트는 `127.0.0.1:5432:5432` 로 바인딩되어 public 인터넷에는 노출되지 않고, 서버 localhost 와 SSH tunnel 경유 접근만 허용

> 주의: 이전에 `/var/lib/postgresql/data` 로 생성된 볼륨이 있으면 PG18 컨테이너가 restart loop에 빠질 수 있다.
> 초기 세팅 단계라면 기존 볼륨을 삭제한 뒤 재배포하는 것이 가장 빠르다.

## Application ↔ DB 연결

권장 구조:

- `fabbit-server` → Dokploy **Application**
- `fabbit-database` → Dokploy **Docker Compose**

앱 환경 변수는 우선 아래처럼 맞춘다.

```dotenv
DB_URL=jdbc:postgresql://host.docker.internal:5432/fabbit
DB_USERNAME=fabbit
DB_PASSWORD=<same-password-as-compose>
```

> `fabbit-database` 는 SSH tunnel/DataGrip 용 접근 호스트가 아니다.
> DataGrip에서는 SSH tunnel을 켜고 **DB Host를 `127.0.0.1`, Port를 `5432`** 로 설정한다.
> 위 `DB_URL` 은 애플리케이션 컨테이너에서 서버 localhost 바인딩 DB에 붙기 위한 값이다.

### DataGrip SSH Tunnel 예시

- Host: `127.0.0.1`
- Port: `5432`
- Database: `fabbit`
- User: `fabbit`
- Password: `<same-password-as-compose>`
- SSH Host: `<OCI public IP>`
- SSH Port: `22`
- SSH User: `<server user>`
- Auth Type: `Key pair`

## GitHub 브랜치 보호 규칙

`release` 에는 아래 정책을 권장한다.

- direct push 금지
- PR merge만 허용
- 일반 기능 브랜치의 직접 머지 금지
- 운영 배포는 `main -> release` PR로만 진행

## Application 런타임 환경 변수

Dokploy에는 최소 아래 값을 넣는다.

```dotenv
PORT=10010
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://<host>:5432/<database>
DB_USERNAME=<username>
DB_PASSWORD=<password>
APP_BASE_DOMAIN=<domain>
APP_INVITATION_BASE_URL=<frontend-url>
APP_JWT_SECRET_KEY=<secret>
STORAGE_ENDPOINT=<s3-endpoint>
STORAGE_ACCESS_KEY=<access-key>
STORAGE_SECRET_KEY=<secret-key>
STORAGE_BUCKET=<bucket>
STORAGE_PUBLIC_URL=<public-url>
LLM_API_KEY=<key>
TURNSTILE_ENABLED=true|false
TURNSTILE_SECRET_KEY=<secret>
SMTP_HOST=<host>
SMTP_PORT=<port>
SMTP_USERNAME=<username>
SMTP_PASSWORD=<password>
SMTP_USE_TLS=true|false
SMTP_FROM_EMAIL=<email>
SMTP_FROM_NAME=<name>
```

> 참고: 현재 저장소의 기존 `docker/docker-compose.prod.yml` 는 현 Spring Boot 서버 배포 정의가 아니라
> 예전 Python/Alembic 기준 명령을 포함하고 있으므로 운영 배포 기준으로 사용하면 안 된다.

## DB 주의사항

이 저장소는 Apache AGE 초기화 SQL을 사용한다. 따라서 앱 배포와 DB provisioning은 분리하는 것이 안전하다.

- 앱: Dokploy GitHub auto deploy
- DB: Dokploy compose/custom image 또는 별도 운영 경로

## 배포 확인 체크리스트

1. Dokploy 앱 branch가 `release` 인가?
2. `/health` 가 200을 반환하는가?
3. `main` 변경은 배포되지 않는가?
4. `release` 변경은 자동 배포되는가?
5. 필요 시 watch paths가 의도대로 동작하는가?
