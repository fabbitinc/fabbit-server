package com.fabbitinc.server.application.auth.service;

import com.fabbitinc.server.application.auth.dto.request.LoginRequest;
import com.fabbitinc.server.application.auth.dto.request.RegisterRequest;
import com.fabbitinc.server.application.auth.dto.response.LoginResponse;
import com.fabbitinc.server.application.auth.dto.response.ScopedLoginResponse;
import com.fabbitinc.server.application.auth.dto.response.TokenResponse;
import com.fabbitinc.server.application.auth.dto.response.UserResponse;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.auth.model.EmailVerification;
import com.fabbitinc.server.domain.auth.model.EmailVerificationStatus;
import com.fabbitinc.server.domain.auth.repository.EmailVerificationRepository;
import com.fabbitinc.server.domain.organization.model.Membership;
import com.fabbitinc.server.domain.organization.repository.MembershipRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthAccountService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordService passwordService;
    private final JwtTokenService jwtTokenService;
    private final FileUrlResolver fileUrlResolver;

    public User registerUser(RegisterRequest request) {
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
        return userRepository.save(user);
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

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                fileUrlResolver.resolve(user.getProfileImageFileKey()),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
