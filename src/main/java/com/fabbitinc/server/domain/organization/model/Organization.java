package com.fabbitinc.server.domain.organization.model;

import com.fabbitinc.server.domain.common.entity.AbstractIdEntity;
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
        this.name = name;
    }

    public void changeProfileImage(String profileImageFileKey) {
        this.profileImageFileKey = profileImageFileKey;
    }

    public void removeProfileImage() {
        this.profileImageFileKey = null;
    }

    public List<Membership> getMemberships() {
        return List.copyOf(memberships);
    }
}
