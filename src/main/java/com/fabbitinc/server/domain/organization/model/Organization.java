package com.fabbitinc.server.domain.organization.model;

import com.fabbitinc.server.domain.common.entity.AbstractIdEntity;
import com.fabbitinc.server.domain.common.entity.AggregateRoot;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
public class Organization extends AbstractIdEntity implements AggregateRoot {

    public static final String CODE_ORGANIZATION_SLUG_REQUIRED = "ORGANIZATION_SLUG_REQUIRED";
    public static final String CODE_ORGANIZATION_SLUG_TOO_LONG = "ORGANIZATION_SLUG_TOO_LONG";
    public static final String CODE_ORGANIZATION_NAME_REQUIRED = "ORGANIZATION_NAME_REQUIRED";
    public static final String CODE_ORGANIZATION_NAME_TOO_LONG = "ORGANIZATION_NAME_TOO_LONG";
    public static final String CODE_ORGANIZATION_OWNER_REQUIRED = "ORGANIZATION_OWNER_REQUIRED";
    public static final String CODE_ORGANIZATION_STORAGE_USAGE_INVALID = "ORGANIZATION_STORAGE_USAGE_INVALID";
    public static final String CODE_ORGANIZATION_MEMBERSHIP_REQUIRED = "ORGANIZATION_MEMBERSHIP_REQUIRED";
    public static final String CODE_ORGANIZATION_MEMBERSHIP_MISMATCH = "ORGANIZATION_MEMBERSHIP_MISMATCH";
    public static final String CODE_ORGANIZATION_LAST_OWNER_ROLE_CHANGE_FORBIDDEN = "ORGANIZATION_LAST_OWNER_ROLE_CHANGE_FORBIDDEN";

    private static final int MAX_SLUG_LENGTH = 50;
    private static final int MAX_NAME_LENGTH = 100;

    @Column(name = "slug", nullable = false, unique = true, length = 50)
    private String slug;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", insertable = false, updatable = false)
    private User owner;

    @Column(name = "industry", length = 50)
    private String industry;

    @Column(name = "team_size", length = 20)
    private String teamSize;

    @Column(name = "storage_bytes_used", nullable = false)
    private long storageBytesUsed;

    @Column(name = "used_members", nullable = false)
    private int usedMembers;

    @Column(name = "profile_image_file_key")
    private String profileImageFileKey;

    @OneToMany(mappedBy = "organization", fetch = FetchType.LAZY)
    private List<Membership> memberships = new ArrayList<>();

    private Organization(String slug, String name, UUID ownerId, String industry, String teamSize) {
        super(UuidV7Generator.next());
        this.slug = requireSlug(slug);
        this.name = requireName(name);
        this.ownerId = requireOwnerId(ownerId);
        this.industry = normalizeOptionalText(industry);
        this.teamSize = normalizeOptionalText(teamSize);
        this.storageBytesUsed = 0L;
        this.usedMembers = 0;
    }

    public static Organization create(String slug, String name, UUID ownerId, String industry, String teamSize) {
        return new Organization(slug, name, ownerId, industry, teamSize);
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

    public Membership addMember(UUID userId, MembershipRole role, String jobRole) {
        Membership membership = Membership.create(this, userId, role, jobRole);
        memberships.add(membership);
        return membership;
    }

    public void reserveMemberSeat() {
        usedMembers++;
    }

    public void releaseMemberSeat() {
        if (usedMembers > 0) {
            usedMembers--;
        }
    }

    public void addStorageUsage(long bytes) {
        long amount = requireNonNegative(bytes, CODE_ORGANIZATION_STORAGE_USAGE_INVALID, "스토리지 사용량은 0 이상이어야 합니다");
        storageBytesUsed += amount;
    }

    public void reduceStorageUsage(long bytes) {
        long amount = requireNonNegative(bytes, CODE_ORGANIZATION_STORAGE_USAGE_INVALID, "스토리지 사용량은 0 이상이어야 합니다");
        storageBytesUsed = Math.max(0L, storageBytesUsed - amount);
    }

    public void changeMemberRole(Membership membership, MembershipRole newRole, long ownerCount) {
        Membership target = requireMembership(membership);
        if (getId().equals(target.getOrgId())) {
            if (target.getRole() == MembershipRole.OWNER && newRole != MembershipRole.OWNER && ownerCount <= 1) {
                throw new DomainException(CODE_ORGANIZATION_LAST_OWNER_ROLE_CHANGE_FORBIDDEN, "마지막 소유자의 역할은 변경할 수 없습니다");
            }
            target.changeRole(newRole);
            return;
        }
        throw new DomainException(CODE_ORGANIZATION_MEMBERSHIP_MISMATCH, "다른 조직의 멤버 역할은 변경할 수 없습니다");
    }

    public List<Membership> getMemberships() {
        return List.copyOf(memberships);
    }

    private String requireSlug(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_ORGANIZATION_SLUG_REQUIRED, "워크스페이스 slug는 필수입니다");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_SLUG_LENGTH) {
            throw new DomainException(CODE_ORGANIZATION_SLUG_TOO_LONG, "워크스페이스 slug는 50자 이하여야 합니다");
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

    private UUID requireOwnerId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_ORGANIZATION_OWNER_REQUIRED, "조직 소유자는 필수입니다");
        }
        return value;
    }

    private Membership requireMembership(Membership membership) {
        if (membership == null) {
            throw new DomainException(CODE_ORGANIZATION_MEMBERSHIP_REQUIRED, "조직 멤버십은 필수입니다");
        }
        return membership;
    }

    private long requireNonNegative(long value, String code, String message) {
        if (value < 0) {
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
