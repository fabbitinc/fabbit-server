package com.fabbitinc.server.domain.organization.model;

import com.fabbitinc.server.domain.common.entity.AbstractIdEntity;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "organizations",
        indexes = {
                @Index(name = "ix_organizations_owner_id", columnList = "owner_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Organization extends AbstractIdEntity {

    @Column(name = "slug", nullable = false, unique = true, length = 50)
    private String slug;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "owner_id", nullable = false)
    private java.util.UUID ownerId;

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

    public Organization(
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
        this.slug = slug;
        this.name = name;
        this.ownerId = ownerId;
        this.industry = industry;
        this.teamSize = teamSize;
        this.planType = planType;
        this.maxMembers = maxMembers;
        this.planCreditsRemaining = planCreditsRemaining;
        this.bonusCreditsRemaining = 0;
        this.storageBytesLimit = storageBytesLimit;
        this.storageBytesUsed = 0L;
        this.allowStorageOverage = false;
        this.usedMembers = 0;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void setProfileImage(String profileImageFileKey) {
        this.profileImageFileKey = profileImageFileKey;
    }

    public void removeProfileImage() {
        this.profileImageFileKey = null;
    }
}
