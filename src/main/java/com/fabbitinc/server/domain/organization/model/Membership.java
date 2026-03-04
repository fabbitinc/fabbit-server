package com.fabbitinc.server.domain.organization.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
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

import java.util.UUID;

@Getter
@Entity
@Table(
        name = "memberships",
        schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_memberships_user_id_org_id", columnNames = {"user_id", "org_id"})
        },
        indexes = {
                @Index(name = "ix_memberships_org_id", columnList = "org_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Membership extends AbstractCreatedEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MembershipRole role;

    @Column(name = "job_role", length = 50)
    private String jobRole;

    public Membership(UUID userId, UUID orgId, MembershipRole role, String jobRole) {
        super(UuidV7Generator.next());
        this.userId = userId;
        this.orgId = orgId;
        this.role = role;
        this.jobRole = jobRole;
    }

    public void changeRole(MembershipRole role) {
        this.role = role;
    }
}
