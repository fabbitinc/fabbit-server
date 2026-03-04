package com.fabbitinc.server.application.auth.service;

import com.fabbitinc.server.application.auth.dto.request.LoginRequest;
import com.fabbitinc.server.application.auth.dto.request.RegisterRequest;
import com.fabbitinc.server.application.auth.dto.response.LoginResponse;
import com.fabbitinc.server.application.auth.dto.response.OrganizationResponse;
import com.fabbitinc.server.application.auth.dto.response.RegisterResponse;
import com.fabbitinc.server.application.auth.dto.response.ScopedLoginResponse;
import com.fabbitinc.server.application.auth.dto.response.TokenResponse;
import com.fabbitinc.server.application.auth.dto.response.UserResponse;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.auth.model.EmailVerification;
import com.fabbitinc.server.domain.auth.model.EmailVerificationStatus;
import com.fabbitinc.server.domain.auth.repository.EmailVerificationRepository;
import com.fabbitinc.server.domain.organization.model.Membership;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.organization.model.OrganizationPlans;
import com.fabbitinc.server.domain.organization.model.PlanLimits;
import com.fabbitinc.server.domain.organization.model.PlanType;
import com.fabbitinc.server.domain.organization.model.WorkspaceSlugPolicy;
import com.fabbitinc.server.domain.organization.repository.MembershipRepository;
import com.fabbitinc.server.domain.organization.repository.OrganizationRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthAccountService {

    private static final long GB_TO_BYTES = 1_000_000_000L;

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordService passwordService;
    private final JwtTokenService jwtTokenService;

    public RegisterResponse register(RegisterRequest request) {
        EmailVerification verification = validateAndConsumeVerification(
                request.verificationToken(),
                request.code()
        );

        String email = verification.getEmail();
        if (userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.ALREADY_EXISTS, "이미 가입된 이메일입니다");
        }

        User user = new User(
                email,
                passwordService.hash(request.password()),
                request.fullName()
        );
        user = userRepository.save(user);

        PlanType planType = resolvePlanType(request.planType());
        PlanLimits limits = OrganizationPlans.limits().get(planType);

        String slug = resolveAvailableSlug(request.slug(), request.orgName());

        Organization organization = new Organization(
                slug,
                request.orgName(),
                user.getId(),
                request.industry(),
                request.teamSize(),
                planType,
                limits.maxMembers(),
                limits.aiCredits(),
                limits.storageGb() * GB_TO_BYTES
        );
        organization = organizationRepository.save(organization);

        Membership ownerMembership = new Membership(
                user.getId(),
                organization.getId(),
                MembershipRole.OWNER,
                request.teamSize()
        );
        membershipRepository.save(ownerMembership);

        TokenResponse tokens = jwtTokenService.issueTokens(
                user.getId(),
                user.getEmail(),
                organization.getId(),
                MembershipRole.OWNER.name()
        );

        return new RegisterResponse(
                toUserResponse(user),
                toOrganizationResponse(organization),
                tokens
        );
    }

    public Object login(LoginRequest request, String slug) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS, "이메일 또는 비밀번호가 올바르지 않습니다"));

        if (!passwordService.matches(request.password(), user.getHashedPassword())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS, "이메일 또는 비밀번호가 올바르지 않습니다");
        }

        if (slug == null || slug.isBlank()) {
            return new ScopedLoginResponse(
                    toUserResponse(user),
                    jwtTokenService.issueScopedToken(user.getId(), user.getEmail(), "create_org")
            );
        }

        Membership membership = membershipRepository.findByUserIdAndOrganizationSlug(user.getId(), slug)
                .orElseThrow(() -> new AppException(ErrorCode.FORBIDDEN, "해당 워크스페이스 접근 권한이 없습니다"));

        TokenResponse tokens = jwtTokenService.issueTokens(
                user.getId(),
                user.getEmail(),
                membership.getOrgId(),
                membership.getRole().name()
        );

        return new LoginResponse(toUserResponse(user), tokens);
    }

    private EmailVerification validateAndConsumeVerification(String verificationToken, String code) {
        String tokenHash = TokenHashingUtils.sha256(verificationToken);
        String codeHash = TokenHashingUtils.sha256(code);

        EmailVerification verification = emailVerificationRepository
                .findFirstByVerificationTokenHashAndCodeHashAndStatus(
                        tokenHash,
                        codeHash,
                        EmailVerificationStatus.VERIFIED
                )
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_VERIFICATION, "유효하지 않은 인증 정보입니다"));

        if (verification.isExpired(Instant.now())) {
            throw new AppException(ErrorCode.CODE_EXPIRED, "인증이 만료되었습니다. 다시 인증해 주세요");
        }

        verification.use();
        return emailVerificationRepository.save(verification);
    }

    private String resolveAvailableSlug(String requestedSlug, String orgName) {
        if (requestedSlug != null && !requestedSlug.isBlank()) {
            String normalized = requestedSlug.trim().toLowerCase(Locale.ROOT);
            validateSlugOrThrow(normalized);
            if (organizationRepository.existsBySlug(normalized)) {
                throw new AppException(ErrorCode.ALREADY_EXISTS, "이미 사용 중인 워크스페이스 주소입니다");
            }
            return normalized;
        }

        String base = slugify(orgName);
        validateSlugOrThrow(base);

        if (!organizationRepository.existsBySlug(base)) {
            return base;
        }

        for (int i = 0; i < 100; i++) {
            String candidate = base + "-" + UUID.randomUUID().toString().substring(0, 4);
            String error = WorkspaceSlugPolicy.validateFormat(candidate);
            if (error == null && !organizationRepository.existsBySlug(candidate)) {
                return candidate;
            }
        }

        throw new AppException(ErrorCode.ALREADY_EXISTS, "워크스페이스 주소를 생성할 수 없습니다");
    }

    private void validateSlugOrThrow(String slug) {
        String error = WorkspaceSlugPolicy.validateFormat(slug);
        if (error != null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, error);
        }
    }

    private String slugify(String orgName) {
        String normalized = orgName == null ? "" : orgName.trim().toLowerCase(Locale.ROOT);
        String slug = normalized
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "")
                .replaceAll("-{2,}", "-");

        if (slug.length() > 50) {
            slug = slug.substring(0, 50).replaceAll("-+$", "");
        }
        if (slug.length() < 3) {
            slug = "workspace";
        }
        return slug;
    }

    private PlanType resolvePlanType(String rawPlanType) {
        if (rawPlanType == null || rawPlanType.isBlank()) {
            return PlanType.STARTER;
        }
        String normalized = rawPlanType.trim().toUpperCase(Locale.ROOT);
        if ("FREE".equals(normalized)) {
            return PlanType.STARTER;
        }
        try {
            return PlanType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "유효하지 않은 plan_type입니다");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName());
    }

    private OrganizationResponse toOrganizationResponse(Organization organization) {
        return new OrganizationResponse(
                organization.getId(),
                organization.getSlug(),
                organization.getName(),
                organization.getPlanType().name(),
                organization.getProfileImageFileKey()
        );
    }
}
