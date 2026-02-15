# 🔐 Step 1 & 2: 계정 생성 및 워크스페이스 프로비저닝

본 문서는 사용자의 가입부터 전용 데이터 공간(Tenant Schema)이 준비되기까지의 기술적 절차와 데이터베이스 설계를 정의합니다.

---

## 1. 개요 (Overview)

사용자가 이메일 인증을 통해 가입하고, 자신의 조직(Workspace)을 생성함과 동시에 물리적으로 격리된 테넌트 스키마와 그래프 공간을 자동으로 할당받는 과정을 다룹니다.

---

## 2. 데이터베이스 설계 (Public Schema)

자체 인증 및 조직 관리를 위해 `public` 스키마에 아래 테이블을 정의합니다.

### 2.1 주요 테이블 명세

| 테이블명          | 용도                    | 핵심 컬럼                                                         |
| :---------------- | :---------------------- | :---------------------------------------------------------------- |
| **users**         | 사용자 계정 정보        | `id` (UUID), `email`, `hashed_password`, `full_name`, `is_active` |
| **organizations** | 조직(워크스페이스) 정보 | `id` (UUID), `slug` (URL용), `name`, `owner_id`                   |
| **memberships**   | 유저-조직 매핑 (RBAC)   | `user_id`, `org_id`, `role` (ADMIN, MEMBER)                       |

---

## 3. 온보딩 시퀀스 (Technical Sequence)

### [Step 1] 계정 및 조직 생성 (Public Action)

1. **User Signup**: 이메일/비밀번호를 통해 `public.users`에 레코드 생성. (Argon2 또는 bcrypt 해싱)
2. **Organization Creation**: 사용자가 입력한 조직명을 바탕으로 `public.organizations`에 레코드 생성.
3. **Membership Linking**: 가입한 사용자를 해당 조직의 `ADMIN`으로 등록.

### [Step 2] 테넌트 프로비저닝 (Infrastructure Action)

조직 생성이 성공하면 백엔드에서 즉시 다음 작업을 수행합니다.

1. **Schema Creation**: `CREATE SCHEMA tenant_{org_id}` 명령 실행.
2. **AGE Graph Initialization**:
   - `SELECT create_graph('tenant_{org_id}')`를 호출하여 해당 스키마 내에 그래프 공간 확보.
3. **Internal Table Setup**:
   - 테넌트 스키마 내부에 프로젝트(`projects`), 폴더(`folders`), 도면 메타데이터(`drawings`) 테이블을 생성.
4. **Base Ontology Loading**:
   - `base_ontology.py`에 정의된 핵심 라벨(Part, BOM 등)에 대한 인덱스를 미리 생성.

---

## 4. 인증 및 세션 관리 (Auth & Session)

### 4.1 JWT 발급 및 검증

- 로그인 성공 시 `access_token` 발급.
- 토큰 페이로드(Payload)에는 `user_id`와 현재 접속 중인 `org_id`를 포함.
- **Security**: 모든 API 요청 시 JWT를 검증하며, `request.state.org_id`에 조직 정보를 주입함.

### 4.2 Database Switching (Tenant Middleware)

- FastAPI의 Dependency Injection(`Depends`)을 활용.
- `org_id`를 기반으로 `SET search_path TO tenant_{org_id}, public` 명령을 실행한 DB 세션을 각 요청에 할당.

---

## 5. 예외 처리 및 복구 (Error Handling)

- **Provisioning Failure**: 스키마 생성 중 에러 발생 시, `public` 스키마에 생성된 조직 레코드도 Rollback 처리하여 데이터 일관성 유지.
- **Slug 중복**: 이미 존재하는 조직 슬러그(Slug) 입력 시 사용자에게 알림 및 자동 추천 기능 제공.

---

## 6. 향후 과제

- [ ] 비밀번호 재설정 및 이메일 인증 메일 발송 로직
- [ ] 조직 초대 링크 생성 및 수락 워크플로우
- [ ] 테넌트 스키마 생성 여부를 확인하는 헬스체크 API
