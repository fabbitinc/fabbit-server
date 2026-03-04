package com.fabbitinc.server.domain.auth.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
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

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

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

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    public Invitation(
            UUID orgId,
            String email,
            MembershipRole role,
            String tokenHash,
            UUID invitedBy,
            Instant expiresAt
    ) {
        super(UuidV7Generator.next());
        this.orgId = orgId;
        this.email = email;
        this.role = role;
        this.tokenHash = tokenHash;
        this.status = InvitationStatus.PENDING;
        this.invitedBy = invitedBy;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public void accept(Instant now) {
        this.status = InvitationStatus.ACCEPTED;
        this.acceptedAt = now;
    }

    public void cancel() {
        this.status = InvitationStatus.CANCELLED;
    }
}
