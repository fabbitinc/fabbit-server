package com.fabbitinc.server.domain.auth.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.organization.model.Organization;
import com.fabbitinc.server.domain.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(
        name = "invitations",
        schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_invitations_org_id_email", columnNames = {"org_id", "email"}),
                @UniqueConstraint(name = "uq_invitations_token_hash", columnNames = "token_hash")
        },
        indexes = {
                @Index(name = "ix_invitations_org_id", columnList = "org_id"),
                @Index(name = "ix_invitations_invited_by", columnList = "invited_by")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Invitation extends AbstractCreatedEntity {

    public static final String CODE_INVITATION_ORG_REQUIRED = "INVITATION_ORG_REQUIRED";
    public static final String CODE_INVITATION_EMAIL_REQUIRED = "INVITATION_EMAIL_REQUIRED";
    public static final String CODE_INVITATION_ROLE_REQUIRED = "INVITATION_ROLE_REQUIRED";
    public static final String CODE_INVITATION_TOKEN_REQUIRED = "INVITATION_TOKEN_REQUIRED";
    public static final String CODE_INVITATION_INVITER_REQUIRED = "INVITATION_INVITER_REQUIRED";
    public static final String CODE_INVITATION_EXPIRES_AT_REQUIRED = "INVITATION_EXPIRES_AT_REQUIRED";
    public static final String CODE_INVITATION_TIME_REQUIRED = "INVITATION_TIME_REQUIRED";
    public static final String CODE_INVITATION_INVALID_STATE = "INVITATION_INVALID_STATE";

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", insertable = false, updatable = false)
    private Organization _organizationRelation;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MembershipRole role;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InvitationStatus status;

    @Column(name = "invited_by", nullable = false)
    private UUID invitedBy;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by", insertable = false, updatable = false)
    private User _inviterRelation;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    private Invitation(
            UUID orgId,
            String email,
            MembershipRole role,
            String tokenHash,
            UUID invitedBy,
            Instant expiresAt
    ) {
        super(UuidV7Generator.next());
        this.orgId = requireOrgId(orgId);
        this.email = requireEmail(email);
        this.role = requireRole(role);
        this.tokenHash = requireTokenHash(tokenHash);
        this.status = InvitationStatus.PENDING;
        this.invitedBy = requireInvitedBy(invitedBy);
        this.expiresAt = requireExpiresAt(expiresAt);
    }

    public static Invitation create(
            UUID orgId,
            String email,
            MembershipRole role,
            String tokenHash,
            UUID invitedBy,
            Instant expiresAt
    ) {
        return new Invitation(orgId, email, role, tokenHash, invitedBy, expiresAt);
    }

    public boolean isExpired(Instant now) {
        Instant requiredNow = requireNow(now);
        return requiredNow.isAfter(expiresAt);
    }

    public void accept(Instant now) {
        Instant requiredNow = requireNow(now);
        if (this.status != InvitationStatus.PENDING) {
            throw new DomainException(CODE_INVITATION_INVALID_STATE, "대기 중인 초대만 수락할 수 있습니다");
        }
        if (requiredNow.isAfter(expiresAt)) {
            throw new DomainException(CODE_INVITATION_INVALID_STATE, "만료된 초대는 수락할 수 없습니다");
        }
        this.status = InvitationStatus.ACCEPTED;
        this.acceptedAt = requiredNow;
    }

    public void cancel() {
        if (this.status != InvitationStatus.PENDING) {
            throw new DomainException(CODE_INVITATION_INVALID_STATE, "대기 중인 초대만 취소할 수 있습니다");
        }
        this.status = InvitationStatus.CANCELLED;
    }

    private UUID requireOrgId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_INVITATION_ORG_REQUIRED, "조직 ID는 필수입니다");
        }
        return value;
    }

    private String requireEmail(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_INVITATION_EMAIL_REQUIRED, "이메일은 필수입니다");
        }
        return value.trim();
    }

    private MembershipRole requireRole(MembershipRole value) {
        if (value == null) {
            throw new DomainException(CODE_INVITATION_ROLE_REQUIRED, "역할은 필수입니다");
        }
        return value;
    }

    private String requireTokenHash(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_INVITATION_TOKEN_REQUIRED, "토큰 해시는 필수입니다");
        }
        return value;
    }

    private UUID requireInvitedBy(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_INVITATION_INVITER_REQUIRED, "초대한 사용자 ID는 필수입니다");
        }
        return value;
    }

    private Instant requireExpiresAt(Instant value) {
        if (value == null) {
            throw new DomainException(CODE_INVITATION_EXPIRES_AT_REQUIRED, "만료 시각은 필수입니다");
        }
        return value;
    }

    private Instant requireNow(Instant value) {
        if (value == null) {
            throw new DomainException(CODE_INVITATION_TIME_REQUIRED, "현재 시각은 필수입니다");
        }
        return value;
    }
}
