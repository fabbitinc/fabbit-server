package com.fabbitinc.server.application.auth.service;

import com.fabbitinc.server.application.auth.port.AuthEmailPort;
import com.fabbitinc.server.application.auth.port.TurnstilePort;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.config.AppProperties;
import com.fabbitinc.server.domain.auth.model.EmailVerification;
import com.fabbitinc.server.domain.auth.model.EmailVerificationStatus;
import com.fabbitinc.server.domain.auth.repository.EmailVerificationRepository;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthVerificationService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final AuthEmailPort authEmailPort;
    private final TurnstilePort turnstilePort;
    private final AppProperties appProperties;

    public void sendVerification(String rawEmail, String turnstileToken) {
        turnstilePort.verify(turnstileToken);
        String email = normalizeEmail(rawEmail);

        if (userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.ALREADY_EXISTS, "이미 가입된 이메일입니다");
        }

        Instant now = Instant.now();
        emailVerificationRepository
                .findFirstByEmailAndStatusOrderByCreatedAtDesc(email, EmailVerificationStatus.PENDING)
                .ifPresent(existing -> {
                    try {
                        existing.ensureResendable(now, appProperties.emailVerificationCooldownSeconds());
                    } catch (DomainException ex) {
                        throw toAppException(ex);
                    }
                });

        emailVerificationRepository.deleteByEmailAndStatus(email, EmailVerificationStatus.PENDING);

        String code = generateCode();
        String codeHash = TokenHashingUtils.sha256(code);
        Instant expiresAt = now.plus(Duration.ofMinutes(appProperties.emailVerificationExpireMinutes()));

        EmailVerification verification = EmailVerification.createPending(email, codeHash, expiresAt);
        emailVerificationRepository.save(verification);

        authEmailPort.sendVerificationCode(email, code);
    }

    public VerifyEmailOutput verifyEmail(String rawEmail, String rawCode) {
        String email = normalizeEmail(rawEmail);
        String codeHash = TokenHashingUtils.sha256(rawCode);
        Instant now = Instant.now();

        EmailVerification verification = emailVerificationRepository
                .findFirstByEmailAndCodeHashAndStatus(email, codeHash, EmailVerificationStatus.PENDING)
                .orElse(null);

        if (verification == null) {
            emailVerificationRepository
                    .findFirstByEmailAndStatusOrderByCreatedAtDesc(email, EmailVerificationStatus.PENDING)
                    .ifPresent(pending -> {
                        try {
                            pending.registerFailedAttempt(appProperties.emailVerificationMaxAttempts());
                            emailVerificationRepository.save(pending);
                        } catch (DomainException ex) {
                            throw toAppException(ex);
                        }
                    });

            throw new AppException(ErrorCode.INVALID_CODE, "인증코드가 올바르지 않습니다");
        }

        try {
            verification.ensureVerifiable(now, appProperties.emailVerificationMaxAttempts());
        } catch (DomainException ex) {
            throw toAppException(ex);
        }

        String rawVerificationToken = UUID.randomUUID().toString().replace("-", "");
        verification.verify(TokenHashingUtils.sha256(rawVerificationToken));
        emailVerificationRepository.save(verification);

        return new VerifyEmailOutput(rawVerificationToken, email);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateCode() {
        int number = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        return "%06d".formatted(number);
    }

    private AppException toAppException(DomainException ex) {
        return switch (ex.getDomainCode()) {
            case EmailVerification.CODE_VERIFICATION_COOLDOWN ->
                    new AppException(ErrorCode.RATE_LIMITED, ex.getMessage());
            case EmailVerification.CODE_VERIFICATION_EXPIRED ->
                    new AppException(ErrorCode.CODE_EXPIRED, ex.getMessage());
            case EmailVerification.CODE_VERIFICATION_MAX_ATTEMPTS ->
                    new AppException(ErrorCode.MAX_ATTEMPTS_EXCEEDED, ex.getMessage());
            default ->
                    new AppException(ErrorCode.INVALID_STATE, ex.getMessage());
        };
    }

    public record VerifyEmailOutput(
            String verificationToken,
            String email
    ) {
    }
}
