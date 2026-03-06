package com.fabbitinc.server.domain.organization.model;

import com.fabbitinc.server.domain.common.entity.AbstractIdEntity;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(
        name = "organizations",
        schema = "public",
        indexes = {
                @Index(name = "ix_organizations_owner_id", columnList = "owner_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Organization extends AbstractIdEntity {

    public static final String CODE_ORGANIZATION_SLUG_REQUIRED = "ORGANIZATION_SLUG_REQUIRED";
    public static final String CODE_ORGANIZATION_SLUG_TOO_LONG = "ORGANIZATION_SLUG_TOO_LONG";
    public static final String CODE_ORGANIZATION_NAME_REQUIRED = "ORGANIZATION_NAME_REQUIRED";
    public static final String CODE_ORGANIZATION_NAME_TOO_LONG = "ORGANIZATION_NAME_TOO_LONG";
    public static final String CODE_ORGANIZATION_OWNER_REQUIRED = "ORGANIZATION_OWNER_REQUIRED";
    public static final String CODE_ORGANIZATION_PLAN_REQUIRED = "ORGANIZATION_PLAN_REQUIRED";
    public static final String CODE_ORGANIZATION_MAX_MEMBERS_INVALID = "ORGANIZATION_MAX_MEMBERS_INVALID";
    public static final String CODE_ORGANIZATION_PLAN_CREDITS_INVALID = "ORGANIZATION_PLAN_CREDITS_INVALID";
    public static final String CODE_ORGANIZATION_STORAGE_LIMIT_INVALID = "ORGANIZATION_STORAGE_LIMIT_INVALID";

    private static final int MAX_SLUG_LENGTH = 50;
    private static final int MAX_NAME_LENGTH = 100;

    @Column(name = "slug", nullable = false, unique = true, length = 50)
    private String slug;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "owner_id", nullable = false)
    private java.util.UUID ownerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", insertable = false, updatable = false)
    private User owner;

    @Column(name = "industry", length = 50)
    private String industry;

    @Column(name = "team_size", length = 20)
    private String teamSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false, length = 20)
    private PlanType planType;

    @Column(name = "max_members", nullable = false)
    private int maxMembers;

    @Column(name = "plan_credits_remaining", nullable = false)
    private int planCreditsRemaining;

    @Column(name = "bonus_credits_remaining", nullable = false)
    private int bonusCreditsRemaining;

    @Column(name = "storage_bytes_limit", nullable = false)
    private long storageBytesLimit;

    @Column(name = "storage_bytes_used", nullable = false)
    private long storageBytesUsed;

    @Column(name = "allow_storage_overage", nullable = false)
    private boolean allowStorageOverage;

    @Column(name = "used_members", nullable = false)
    private int usedMembers;

    @Column(name = "profile_image_file_key")
    private String profileImageFileKey;

    @OneToMany(mappedBy = "organization", fetch = FetchType.LAZY)
    private List<Membership> memberships = new ArrayList<>();

    private Organization(
            String slug,
            String name,
            java.util.UUID ownerId,
            String industry,
            String teamSize,
            PlanType planType,
            int maxMembers,
            int planCreditsRemaining,
            long storageBytesLimit
    ) {
        super(UuidV7Generator.next());
        this.slug = requireSlug(slug);
        this.name = requireName(name);
        this.ownerId = requireOwnerId(ownerId);
        this.industry = normalizeOptionalText(industry);
        this.teamSize = normalizeOptionalText(teamSize);
        this.planType = requirePlanType(planType);
        this.maxMembers = requirePositive(maxMembers, CODE_ORGANIZATION_MAX_MEMBERS_INVALID, "최대 멤버 수는 1 이상이어야 합니다");
        this.planCreditsRemaining = requireNonNegative(planCreditsRemaining, CODE_ORGANIZATION_PLAN_CREDITS_INVALID, "플랜 크레딧은 0 이상이어야 합니다");
        this.bonusCreditsRemaining = 0;
        this.storageBytesLimit = requireNonNegative(storageBytesLimit, CODE_ORGANIZATION_STORAGE_LIMIT_INVALID, "스토리지 한도는 0 이상이어야 합니다");
        this.storageBytesUsed = 0L;
        this.allowStorageOverage = false;
        this.usedMembers = 0;
    }

    public static Organization create(
            String slug,
            String name,
            java.util.UUID ownerId,
            String industry,
            String teamSize,
            PlanType planType,
            int maxMembers,
            int planCreditsRemaining,
            long storageBytesLimit
    ) {
        return new Organization(
                slug,
                name,
                ownerId,
                industry,
                teamSize,
                planType,
                maxMembers,
                planCreditsRemaining,
                storageBytesLimit
        );
    }

    public void rename(String name) {
        this.name = requireName(name);
    }

    public void changeProfileImage(String profileImageFileKey) {
        this.profileImageFileKey = normalizeOptionalText(profileImageFileKey);
    }

    public void removeProfileImage() {
        this.profileImageFileKey = null;
    }

    public List<Membership> getMemberships() {
        return List.copyOf(memberships);
    }

    private String requireSlug(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_ORGANIZATION_SLUG_REQUIRED, "워크스페이스 주소는 필수입니다");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_SLUG_LENGTH) {
            throw new DomainException(CODE_ORGANIZATION_SLUG_TOO_LONG, "워크스페이스 주소는 50자 이하여야 합니다");
        }
        return trimmed;
    }

    private String requireName(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_ORGANIZATION_NAME_REQUIRED, "조직 이름은 필수입니다");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new DomainException(CODE_ORGANIZATION_NAME_TOO_LONG, "조직 이름은 100자 이하여야 합니다");
        }
        return trimmed;
    }

    private java.util.UUID requireOwnerId(java.util.UUID value) {
        if (value == null) {
            throw new DomainException(CODE_ORGANIZATION_OWNER_REQUIRED, "소유자 ID는 필수입니다");
        }
        return value;
    }

    private PlanType requirePlanType(PlanType value) {
        if (value == null) {
            throw new DomainException(CODE_ORGANIZATION_PLAN_REQUIRED, "플랜 타입은 필수입니다");
        }
        return value;
    }

    private int requirePositive(int value, String code, String message) {
        if (value < 1) {
            throw new DomainException(code, message);
        }
        return value;
    }

    private int requireNonNegative(int value, String code, String message) {
        if (value < 0) {
            throw new DomainException(code, message);
        }
        return value;
    }

    private long requireNonNegative(long value, String code, String message) {
        if (value < 0L) {
            throw new DomainException(code, message);
        }
        return value;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
