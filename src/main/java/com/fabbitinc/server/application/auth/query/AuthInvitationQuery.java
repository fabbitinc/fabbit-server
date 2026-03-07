package com.fabbitinc.server.application.auth.query;

import com.fabbitinc.server.application.auth.query.condition.VerifyInvitationCondition;
import com.fabbitinc.server.application.auth.query.result.VerifyInvitationResult;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.auth.model.Invitation;
import com.fabbitinc.server.domain.auth.model.InvitationStatus;
import com.fabbitinc.server.domain.auth.repository.InvitationRepository;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.organization.repository.OrganizationRepository;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthInvitationQuery {

    private final InvitationRepository invitationRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    public VerifyInvitationResult getVerifiedInvitation(VerifyInvitationCondition condition) {
        Invitation invitation = invitationRepository.findByTokenHash(sha256(condition.token()))
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "유효하지 않은 초대입니다"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "이미 처리된 초대입니다");
        }

        if (invitation.isExpired(Instant.now())) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "만료된 초대입니다");
        }

        Organization organization = organizationRepository.findById(invitation.getOrgId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "조직을 찾을 수 없습니다"));

        User inviter = userRepository.findById(invitation.getInvitedBy())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "초대자를 찾을 수 없습니다"));

        boolean existingUser = userRepository.existsByEmail(invitation.getEmail());

        return new VerifyInvitationResult(
                invitation.getEmail(),
                organization.getName(),
                inviter.getFullName(),
                invitation.getRole(),
                existingUser,
                invitation.getExpiresAt()
        );
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다", ex);
        }
    }
}
