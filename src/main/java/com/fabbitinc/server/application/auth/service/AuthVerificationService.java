package com.fabbitinc.server.application.auth.service;

import com.fabbitinc.server.application.auth.dto.request.SendVerificationRequest;
import com.fabbitinc.server.application.auth.dto.request.VerifyEmailRequest;
import com.fabbitinc.server.application.auth.dto.response.SendVerificationResponse;
import com.fabbitinc.server.application.auth.dto.response.VerifyEmailResponse;
import com.fabbitinc.server.application.auth.port.AuthEmailPort;
import com.fabbitinc.server.domain.auth.model.EmailVerification;
import com.fabbitinc.server.domain.auth.model.EmailVerificationStatus;
import com.fabbitinc.server.domain.auth.repository.EmailVerificationRepository;
import com.fabbitinc.server.application.config.AppProperties;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AuthVerificationService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final AuthEmailPort authEmailPort;
    private final AppProperties appProperties;

    public SendVerificationResponse sendVerification(SendVerificationRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.ALREADY_EXISTS, "이미 가입된 이메일입니다");
        }

        Instant now = Instant.now();
        emailVerificationRepository
                .findFirstByEmailAndStatusOrderByCreatedAtDesc(email, EmailVerificationStatus.PENDING)
                .ifPresent(existing -> {
                    long elapsed = Duration.between(existing.getCreatedAt(), now).toSeconds();
                    if (elapsed < appProperties.emailVerificationCooldownSeconds()) {
                        throw new AppException(ErrorCode.RATE_LIMITED, "잠시 후 다시 시도해 주세요");
                    }
                });

        emailVerificationRepository.deleteByEmailAndStatus(email, EmailVerificationStatus.PENDING);

        String code = generateCode();
        String codeHash = TokenHashingUtils.sha256(code);
        Instant expiresAt = now.plus(Duration.ofMinutes(appProperties.emailVerificationExpireMinutes()));

        EmailVerification verification = EmailVerification.createPending(email, codeHash, expiresAt);
        emailVerificationRepository.save(verification);

        authEmailPort.sendVerificationCode(email, code);

        return new SendVerificationResponse("인증코드가 발송되었습니다");
    }

    public VerifyEmailResponse verifyEmail(VerifyEmailRequest request) {
        String email = normalizeEmail(request.email());
        String codeHash = TokenHashingUtils.sha256(request.code());
        Instant now = Instant.now();

        EmailVerification verification = emailVerificationRepository
                .findFirstByEmailAndCodeHashAndStatus(email, codeHash, EmailVerificationStatus.PENDING)
                .orElse(null);

        if (verification == null) {
            emailVerificationRepository
                    .findFirstByEmailAndStatusOrderByCreatedAtDesc(email, EmailVerificationStatus.PENDING)
                    .ifPresent(pending -> {
                        pending.incrementAttempt();
                        emailVerificationRepository.save(pending);

                        if (pending.isMaxAttempts(appProperties.emailVerificationMaxAttempts())) {
                            throw new AppException(
                                    ErrorCode.MAX_ATTEMPTS_EXCEEDED,
                                    "인증 시도 횟수를 초과했습니다. 인증코드를 재발송해 주세요"
                            );
                        }
                    });

            throw new AppException(ErrorCode.INVALID_CODE, "인증코드가 올바르지 않습니다");
        }

        if (verification.isExpired(now)) {
            throw new AppException(ErrorCode.CODE_EXPIRED, "인증코드가 만료되었습니다. 재발송해 주세요");
        }

        if (verification.isMaxAttempts(appProperties.emailVerificationMaxAttempts())) {
            throw new AppException(
                    ErrorCode.MAX_ATTEMPTS_EXCEEDED,
                    "인증 시도 횟수를 초과했습니다. 인증코드를 재발송해 주세요"
            );
        }

        String rawVerificationToken = UUID.randomUUID().toString().replace("-", "");
        verification.verify(TokenHashingUtils.sha256(rawVerificationToken));
        emailVerificationRepository.save(verification);

        return new VerifyEmailResponse(rawVerificationToken, email);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateCode() {
        int number = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        return "%06d".formatted(number);
    }
}
