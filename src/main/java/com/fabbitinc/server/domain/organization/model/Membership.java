package com.fabbitinc.server.domain.organization.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
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

    public static final String CODE_MEMBERSHIP_USER_REQUIRED = "MEMBERSHIP_USER_REQUIRED";
    public static final String CODE_MEMBERSHIP_ORG_REQUIRED = "MEMBERSHIP_ORG_REQUIRED";
    public static final String CODE_MEMBERSHIP_ROLE_REQUIRED = "MEMBERSHIP_ROLE_REQUIRED";

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", insertable = false, updatable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MembershipRole role;

    @Column(name = "job_role", length = 50)
    private String jobRole;

    private Membership(UUID userId, UUID orgId, MembershipRole role, String jobRole) {
        super(UuidV7Generator.next());
        this.userId = requireUserId(userId);
        this.orgId = requireOrgId(orgId);
        this.role = requireRole(role);
        this.jobRole = jobRole;
    }

    public static Membership create(UUID userId, UUID orgId, MembershipRole role, String jobRole) {
        return new Membership(userId, orgId, role, jobRole);
    }

    public static Membership create(User user, Organization organization, MembershipRole role, String jobRole) {
        if (user == null) {
            throw new DomainException(CODE_MEMBERSHIP_USER_REQUIRED, "사용자 ID는 필수입니다");
        }
        if (organization == null) {
            throw new DomainException(CODE_MEMBERSHIP_ORG_REQUIRED, "조직 ID는 필수입니다");
        }
        Membership membership = new Membership(user.getId(), organization.getId(), role, jobRole);
        membership.user = user;
        membership.organization = organization;
        return membership;
    }

    public static Membership createOwner(UUID userId, UUID orgId) {
        return new Membership(userId, orgId, MembershipRole.OWNER, null);
    }

    public static Membership createOwner(User user, Organization organization) {
        return create(user, organization, MembershipRole.OWNER, null);
    }

    public void changeRole(MembershipRole role) {
        this.role = requireRole(role);
    }

    private UUID requireUserId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_MEMBERSHIP_USER_REQUIRED, "사용자 ID는 필수입니다");
        }
        return value;
    }

    private UUID requireOrgId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_MEMBERSHIP_ORG_REQUIRED, "조직 ID는 필수입니다");
        }
        return value;
    }

    private MembershipRole requireRole(MembershipRole value) {
        if (value == null) {
            throw new DomainException(CODE_MEMBERSHIP_ROLE_REQUIRED, "멤버 역할은 필수입니다");
        }
        return value;
    }
}
