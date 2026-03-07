# 계정/조직 도메인 마이그레이션 추적

## 범위

- 구 FastAPI 기준:
  - `../server/app/modules/auth/service.py`
  - `../server/app/modules/organization/service.py`
  - `../server/app/modules/user/service.py`
  - `../server/app/modules/subscription/service.py`
  - `../server/app/modules/ai_usage/service.py`
- 신 Spring Boot 기준:
  - `src/main/java/com/fabbitinc/server/application/auth/**`
  - `src/main/java/com/fabbitinc/server/application/organization/**`
  - `src/main/java/com/fabbitinc/server/application/user/**`
  - `src/main/java/com/fabbitinc/server/application/member/**`
  - `src/main/java/com/fabbitinc/server/application/aiusage/**`
  - 관련 `presentation`, `domain`, `repository`, `query`

## 전제

- 구 프로젝트에는 이름이 정확히 `usecase.py`인 파일은 없습니다.
- 대신 실제 오케스트레이션 확인을 위해 `../server/app/use_cases/**/*.py`를 보조 근거로 사용했습니다.
- 평가는 정적 코드 비교 기준입니다. 런타임 테스트나 DB 마이그레이션 검증은 포함하지 않습니다.

## 요약

- 함수 단위 매핑 기준 총 `44`개 중 `완료 38`, `부분 3`, `누락 3`입니다.
- 단순 기능 수치로 보면 완성도는 약 `86%`입니다.
- 현재 남은 큰 누락은 조직 스토리지 quota 관리 3종입니다.
- 부분 이행 항목도 실제 운영 리스크가 있습니다.
  - 회원가입 시 Turnstile 재검증 누락
  - 비활성 사용자 로그인 차단 누락
  - 슬러그 자동 생성/예약어 정책 차이

## Auth

| 구 서비스 함수 | 현재 대응 구현 | 상태 | 근거 |
|---|---|---|---|
| `verify_turnstile` | `AuthVerificationService.sendVerification`, `CloudflareTurnstileAdapter.verify` | 부분 | Turnstile 검증은 인증코드 발송 경로에는 존재하지만, 구 `register.py`에서 수행하던 회원가입 직전 Turnstile 검증은 Spring `RegisterUseCase`에서 빠졌습니다. `RegisterRequest.turnstileToken` 필드는 존재하지만 `RegisterCommand`로 전달되지 않습니다. |
| `send_verification_email` | `SendVerificationUseCase`, `AuthVerificationService.sendVerification` | 완료 | 이메일 중복 확인, 쿨다운, PENDING 삭제, 코드 생성, 메일 발송이 모두 구현되어 있습니다. |
| `verify_email` | `VerifyEmailUseCase`, `AuthVerificationService.verifyEmail` | 완료 | 코드 해시 조회, 실패 시 시도 횟수 증가, 만료/최대 시도 검증, verification token 발급이 유지됩니다. |
| `validate_and_consume_verification` | `AuthAccountService.validateAndConsumeVerification` | 완료 | VERIFIED 상태 검증 후 USED 처리하는 흐름이 `registerUser` 내부로 이동했습니다. |
| `issue_tokens` | `JwtTokenService.issueTokenBundle` | 완료 | access/refresh 토큰 발급과 refresh token 저장이 통합 구현되어 있습니다. |
| `issue_scoped_token` | `JwtTokenService.issueScopedToken`, `LoginUseCase` | 완료 | 조직 미선택 로그인 시 `create_org` 스코프 토큰 발급이 유지됩니다. |
| `validate_refresh_token` | `JwtTokenService.refreshTokenBundle` | 완료 | JWT 검증, token type 확인, 저장 토큰 조회, 재사용 감지, 유저/멤버십 조회가 한 메서드로 통합되었습니다. |
| `revoke_refresh_token` | `JwtTokenService.refreshTokenBundle` 내부 `rotate` | 완료 | 구 구현의 "기존 refresh token 폐기 후 신규 발급"이 Spring에서는 `rotate`로 내부 통합되어 동일 효과를 냅니다. |
| `revoke_all_user_tokens` | `JwtTokenService.revokeAllUserTokens` | 완료 | 사용자 전체 refresh token 폐기가 유지됩니다. |
| `logout` | `LogoutUseCase`, `JwtTokenService.revokeAllUserTokens` | 완료 | 구 구현과 동일하게 로그아웃 시 해당 사용자의 전체 refresh token을 무효화합니다. |
| `create_invitation_record` | `CreateInvitationUseCase`, `AuthInvitationService.createInvitationRecord` | 완료 | 역할 검증, 관리 가능 역할 검증, 중복 PENDING 방지, CANCELLED 정리, 초대 토큰 생성이 유지됩니다. |
| `validate_invitation_token` | `AuthInvitationService.validateInvitationToken`, `AuthInvitationQuery.getVerifiedInvitation` | 완료 | PENDING/만료 여부 검증이 수락 경로와 사전 검증 경로에 모두 반영되었습니다. |
| `cancel_invitation` | `CancelInvitationUseCase`, `AuthInvitationService.cancelInvitation` | 완료 | org scope 확인과 PENDING 상태 검증이 유지됩니다. |
| `send_invitation_email` | `AuthInvitationService.sendInvitationEmail` | 완료 | 메일 발송 포트로 위임하는 구조로 유지됩니다. |
| `build_invite_url` | `AuthInvitationService.buildInviteUrl` | 완료 | `{slug}.{baseDomain}` 기반 초대 URL 생성이 유지됩니다. |
| `_send_verification_code_email` | `AuthEmailPort.sendVerificationCode` | 완료 | 구현 위치가 포트/어댑터로 이동했을 뿐 동작은 유지됩니다. |

## Organization

| 구 서비스 함수 | 현재 대응 구현 | 상태 | 근거 |
|---|---|---|---|
| `_slugify` | `OrganizationService.resolveAvailableSlug`, `slugify`, `WorkspaceSlugPolicy` | 부분 | 구 구현은 자동 slug 생성 시 한글 등 non-ASCII를 보존했지만, Spring은 `[a-z0-9\\s-]`만 허용해 비ASCII 조직명이 `org` fallback 또는 UUID suffix로 바뀔 수 있습니다. 또한 `WorkspaceSlugPolicy` 예약어 목록에서 `dev`, `staging`, `test`, `dns`, `vpn`, `proxy`, `gateway` 등이 주석 처리되어 구 정책보다 완화되었습니다. |
| `create_organization` | `CreateOrganizationUseCase`, `OrganizationService.createOrganization`, `SubscriptionApi.createInitialSubscription` | 완료 | 조직 생성, OWNER 멤버십, 좌석 예약, 테넌트 프로비저닝 뒤 초기 ACTIVE 구독 생성까지 포함해 레거시 흐름이 다시 맞춰졌습니다. |
| `switch_org` | `SwitchOrganizationUseCase`, `OrganizationService.switchOrganization` | 완료 | slug 기반 조직 전환과 멤버십 검증 후 토큰 재발급이 유지됩니다. |
| `remove_member` | `RemoveMemberUseCase`, `OrganizationService.removeMember` | 완료 | 자기 자신 제거 금지, 권한 계층 검증, 마지막 OWNER 보호, 좌석 해제가 유지됩니다. |
| `change_member_role` | `ChangeMemberRoleUseCase`, `OrganizationService.changeMemberRole` | 완료 | 자기 역할 변경 금지, 역할 검증, 마지막 OWNER 보호가 유지됩니다. |
| `get_first_membership_or_raise` | `OrganizationService.getFirstMembershipOrThrow`, `JwtTokenService.refreshTokenBundle` | 완료 | refresh 시 첫 멤버십 조회가 유지됩니다. |
| `check_not_member_by_email` | `CreateInvitationUseCase`, `OrganizationService.checkNotMember` | 완료 | 기존 사용자 이메일 초대 시 중복 멤버 검증이 유지됩니다. |
| `get_org_or_raise` | `OrganizationService.getOrgOrThrow` | 완료 | 조직 404 검증이 유지됩니다. |
| `add_member` | `AcceptInvitationUseCase`, `OrganizationApi.addMember`, `OrganizationService.addMember` | 완료 | 중복 멤버 방지와 좌석 예약을 포함한 멤버 추가가 유지됩니다. |
| `check_credit_quota` | `OrganizationService.checkCreditQuota` | 완료 | AI credit 잔량 검증이 유지됩니다. |
| `consume_credits` | `OrganizationService.consumeCredits` | 완료 | `findByIdForUpdate` 후 credit 차감으로 이전 원자적 차감 의도가 유지됩니다. |
| `check_storage_quota` | 대응 구현 미발견 | 누락 | Spring application/service/usecase 어디에서도 storage quota 사전 검증 진입점을 찾지 못했습니다. |
| `consume_storage` | 대응 구현 미발견 | 누락 | `Organization` 엔티티에는 `useStorage` 성격 메서드가 있으나 application/service에서 호출되지 않습니다. |
| `release_storage` | 대응 구현 미발견 | 누락 | 파일 삭제 후 storage 반환에 해당하는 application/service 호출 경로를 찾지 못했습니다. |
| `set_profile_image` | `SetOrganizationProfileImageUseCase`, `OrganizationService.setProfileImage` | 완료 | attachable 파일 검증, 썸네일 변환, 조직 프로필 이미지 연결이 유지됩니다. |
| `delete_profile_image` | `DeleteOrganizationProfileImageUseCase`, `OrganizationService.deleteProfileImage` | 완료 | 조직 프로필 이미지 제거와 연결 파일 soft delete가 유지됩니다. |

## User

| 구 서비스 함수 | 현재 대응 구현 | 상태 | 근거 |
|---|---|---|---|
| `create_user` | `UserService.createUser` | 완료 | 이메일 정규화, 비밀번호 해싱, 사용자 생성이 유지됩니다. |
| `find_or_create_for_invitation` | `UserService.findOrCreateForInvitation` | 완료 | 기존 유저 재사용, 신규 가입 시 비밀번호/이름 필수 검증이 유지됩니다. |
| `authenticate` | `AuthAccountService.authenticate` | 부분 | 이메일/비밀번호 검증은 유지되지만, 구 구현의 `if not user.is_active: raise FORBIDDEN` 규칙이 Spring에는 없습니다. |
| `get_user_by_email` | `UserService.getUserByEmail` | 완료 | 이메일 기준 조회가 유지됩니다. |
| `get_user_or_raise` | `UserService.getUserOrThrow` | 완료 | 사용자 404 검증이 유지됩니다. |
| `update_profile` | `UpdateProfileUseCase`, `UserService.updateProfile` | 완료 | partial update 성격이 유지되고, 결과 응답도 동일한 사용자 정보를 반환합니다. |
| `change_password` | `ChangePasswordUseCase`, `UserService.changePassword` | 완료 | 현재 비밀번호 검증 후 새 비밀번호 해시 저장이 유지됩니다. |
| `set_profile_image` | `SetProfileImageUseCase`, `UserService.setProfileImage` | 완료 | attachable 파일 검증, 썸네일 변환, 사용자 프로필 이미지 연결이 유지됩니다. |
| `delete_profile_image` | `DeleteProfileImageUseCase`, `UserService.deleteProfileImage` | 완료 | 프로필 이미지 제거와 파일 soft delete가 유지됩니다. |
| `_to_update_profile_response` | `UpdateProfileUseCase` 결과 매핑 | 완료 | 응답 조합 책임이 usecase/result로 이동했습니다. |

## Subscription

| 구 서비스 함수 | 현재 대응 구현 | 상태 | 근거 |
|---|---|---|---|
| `create_initial_subscription` | `SubscriptionApi.createInitialSubscription`, `SubscriptionService.createInitialSubscription` | 완료 | 조직 생성 시 ACTIVE 구독을 생성하고, 현재 기간/플랜 한도 스냅샷을 저장합니다. `OrganizationServicePersistenceTest`에서도 저장을 검증합니다. |

## AI Usage

| 구 서비스 함수 | 현재 대응 구현 | 상태 | 근거 |
|---|---|---|---|
| `log_usage` | `AiUsageService.record`, `PreviewMappingUseCase` | 완료 | 구 프로젝트에서 실제 호출처가 `mapping/preview_mapping.py`였고, Spring도 `PreviewMappingUseCase`에서 동일하게 사용량 기록을 남깁니다. |

## 핵심 갭

1. 조직 스토리지 quota 관리가 애플리케이션 계층에서 사라졌습니다.
   - 구 프로젝트는 `check_storage_quota`, `consume_storage`, `release_storage`를 통해 파일 업로드/삭제와 연결했습니다.
   - Spring은 `Organization` 엔티티에 storage 관련 상태와 메서드가 남아 있지만 호출 경로가 없습니다.
   - 결과적으로 파일 업로드가 quota를 우회하거나, 삭제 후 사용량이 회복되지 않을 가능성이 있습니다.

2. 회원가입 Turnstile 재검증이 빠졌습니다.
   - 구 `auth/register.py`는 인증코드 발송 시점과 회원가입 시점 모두 Turnstile을 확인했습니다.
   - Spring은 발송 시점만 검증합니다.

3. 비활성 사용자 로그인 차단 규칙이 빠졌습니다.
   - `User.active` 필드는 남아 있지만 `AuthAccountService.authenticate`는 이를 검사하지 않습니다.

4. slug 정책이 미세하게 달라졌습니다.
   - 자동 slug 생성 시 한글/비ASCII 처리 방식이 바뀌었습니다.
   - 예약어 목록 일부가 누락돼 이전에 막히던 slug가 허용될 수 있습니다.

## 리스크

- 파일 업로드량이 누적돼도 조직 storage 사용량/제한이 갱신되지 않으면 과금·제한 정책이 무력화됩니다.
- 비활성 사용자 로그인 허용은 운영/보안 이슈로 이어질 수 있습니다.
- slug 정책 차이는 이미 운영 중인 URL 정책과 충돌하거나, 이전에 금지한 시스템 reserved slug를 허용하는 문제를 만들 수 있습니다.
