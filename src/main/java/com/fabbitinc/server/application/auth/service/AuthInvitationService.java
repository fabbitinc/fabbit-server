package com.fabbitinc.server.application.auth.service;

import com.fabbitinc.server.application.auth.port.AuthEmailPort;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.config.AppProperties;
import com.fabbitinc.server.domain.auth.model.Invitation;
import com.fabbitinc.server.domain.auth.model.InvitationStatus;
import com.fabbitinc.server.domain.auth.repository.InvitationRepository;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.organization.repository.OrganizationRepository;
import com.fabbitinc.server.domain.subscription.model.SeatType;
import com.fabbitinc.server.domain.user.model.User;
import com.fabbitinc.server.domain.user.repository.UserRepository;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthInvitationService {

    private final InvitationRepository invitationRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final AuthEmailPort authEmailPort;
    private final AppProperties appProperties;

    public CreatedInvitation createInvitationRecord(
            UUID orgId,
            String email,
            UUID invitedBy,
            MembershipRole role,
            SeatType seatType,
            MembershipRole actorRole
    ) {
        if (role == null) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "유효하지 않은 역할입니다");
        }
        MembershipRole invitedRole = role;

        if (actorRole != null && !actorRole.canManage(invitedRole)) {
            throw new AppException(ErrorCode.FORBIDDEN, "해당 역할로 초대할 권한이 없습니다");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        invitationRepository.findByOrgIdAndEmailAndStatus(orgId, normalizedEmail, InvitationStatus.PENDING)
                .ifPresent(existing -> {
                    throw new AppException(ErrorCode.ALREADY_EXISTS, "이미 초대가 발송된 이메일입니다");
                });

        invitationRepository.deleteByOrgIdAndEmailAndStatus(orgId, normalizedEmail, InvitationStatus.CANCELLED);

        String rawToken = createRawToken();
        Invitation invitation = Invitation.create(
                orgId,
                normalizedEmail,
                invitedRole,
                seatType,
                TokenHashingUtils.sha256(rawToken),
                invitedBy,
                Instant.now().plus(appProperties.invitationExpireDays(), ChronoUnit.DAYS)
        );

        Invitation saved = invitationRepository.save(invitation);
        return new CreatedInvitation(saved, rawToken);
    }

    public Invitation validateInvitationToken(String rawToken) {
        Invitation invitation = invitationRepository.findByTokenHash(TokenHashingUtils.sha256(rawToken))
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "유효하지 않은 초대입니다"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "이미 처리된 초대입니다");
        }

        if (invitation.isExpired(Instant.now())) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "만료된 초대입니다");
        }

        return invitation;
    }

    public void cancelInvitation(UUID orgId, UUID invitationId) {
        Invitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "초대를 찾을 수 없습니다"));

        if (!invitation.getOrgId().equals(orgId)) {
            throw new AppException(ErrorCode.NOT_FOUND, "초대를 찾을 수 없습니다");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new AppException(ErrorCode.VALIDATION_ERROR, "대기 중인 초대만 취소할 수 있습니다");
        }

        invitation.cancel();
    }

    public String buildInviteUrl(String token, String slug) {
        URI baseUri = URI.create(appProperties.invitationBaseUrl());
        String port = baseUri.getPort() > 0 ? ":" + baseUri.getPort() : "";
        return baseUri.getScheme() + "://" + slug + "." + appProperties.baseDomain() + port + "/invite/accept?token=" + token;
    }

    public void sendInvitationEmail(String email, String orgName, String inviterName, String inviteUrl) {
        authEmailPort.sendInvitation(email, orgName, inviterName, inviteUrl);
    }

    public Organization getOrganizationOrThrow(UUID orgId) {
        return organizationRepository.findById(orgId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "조직을 찾을 수 없습니다"));
    }

    public User getInviterOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다"));
    }

    private String createRawToken() {
        return UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
    }

    public record CreatedInvitation(Invitation invitation, String rawToken) {
    }
}
