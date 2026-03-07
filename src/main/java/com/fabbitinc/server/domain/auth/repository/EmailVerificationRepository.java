package com.fabbitinc.server.domain.auth.repository;

import com.fabbitinc.server.domain.auth.model.EmailVerification;
import com.fabbitinc.server.domain.auth.model.EmailVerificationStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {

    Optional<EmailVerification> findFirstByEmailAndStatusOrderByCreatedAtDesc(
            String email,
            EmailVerificationStatus status
    );

    Optional<EmailVerification> findFirstByEmailAndCodeHashAndStatus(
            String email,
            String codeHash,
            EmailVerificationStatus status
    );

    Optional<EmailVerification> findFirstByVerificationTokenHashAndCodeHashAndStatus(
            String verificationTokenHash,
            String codeHash,
            EmailVerificationStatus status
    );

    @Modifying
    void deleteByEmailAndStatus(String email, EmailVerificationStatus status);
}
