package com.fabbitinc.server.application.auth.service;

import com.fabbitinc.server.application.auth.policy.PasswordPolicy;
import com.fabbitinc.server.application.auth.service.input.LoginInput;
import com.fabbitinc.server.application.auth.service.input.RegisterUserInput;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.auth.model.EmailVerification;
import com.fabbitinc.server.domain.auth.model.EmailVerificationStatus;
import com.fabbitinc.server.domain.auth.repository.EmailVerificationRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import java.time.Instant;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthAccountService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final PasswordPolicy passwordPolicy;

    public User registerUser(RegisterUserInput input) {
        EmailVerification verification = validateAndConsumeVerification(
                input.verificationToken(),
                input.code()
        );

        String email = verification.getEmail();
        if (userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.ALREADY_EXISTS, "이미 가입된 이메일입니다");
        }

        User user = User.create(
                email,
                passwordPolicy.hash(input.password()),
                input.fullName()
        );
        return userRepository.save(user);
    }

    public User authenticate(LoginInput input) {
        String email = normalizeEmail(input.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS, "이메일 또는 비밀번호가 올바르지 않습니다"));

        if (!passwordPolicy.matches(input.password(), user.getHashedPassword())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS, "이메일 또는 비밀번호가 올바르지 않습니다");
        }

        return user;
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
}
