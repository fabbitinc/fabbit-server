package com.fabbitinc.server.domain.part.model;

import com.fabbitinc.server.domain.common.entity.AbstractAuditableEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import com.fabbitinc.server.domain.team.model.Team;
import com.fabbitinc.server.domain.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "part_default_owners")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartDefaultOwner extends AbstractAuditableEntity {

    public static final String CODE_PART_DEFAULT_OWNER_CATEGORY_TOO_LONG = "PART_DEFAULT_OWNER_CATEGORY_TOO_LONG";
    public static final String CODE_PART_DEFAULT_OWNER_TARGET_REQUIRED = "PART_DEFAULT_OWNER_TARGET_REQUIRED";

    private static final int MAX_CATEGORY_LENGTH = 100;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "default_owner_id")
    private UUID defaultOwnerId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_owner_id", insertable = false, updatable = false)
    private User _defaultOwnerRelation;

    @Column(name = "default_owner_team_id")
    private UUID defaultOwnerTeamId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_owner_team_id", insertable = false, updatable = false)
    private Team _defaultOwnerTeamRelation;

    private PartDefaultOwner(String category, UUID defaultOwnerId, UUID defaultOwnerTeamId) {
        super(UuidV7Generator.next());
        this.category = normalizeCategory(category);
        requireTarget(defaultOwnerId, defaultOwnerTeamId);
        this.defaultOwnerId = defaultOwnerId;
        this.defaultOwnerTeamId = defaultOwnerTeamId;
    }

    public static PartDefaultOwner create(String category, UUID defaultOwnerId, UUID defaultOwnerTeamId) {
        return new PartDefaultOwner(category, defaultOwnerId, defaultOwnerTeamId);
    }

    public void update(UUID defaultOwnerId, UUID defaultOwnerTeamId) {
        requireTarget(defaultOwnerId, defaultOwnerTeamId);
        assignDefaultOwner(defaultOwnerId);
        assignDefaultOwnerTeam(defaultOwnerTeamId);
    }

    public void assignDefaultOwner(UUID defaultOwnerId) {
        this.defaultOwnerId = defaultOwnerId;
        if (this._defaultOwnerRelation != null
                && (defaultOwnerId == null || !defaultOwnerId.equals(this._defaultOwnerRelation.getId()))) {
            this._defaultOwnerRelation = null;
        }
    }

    public void assignDefaultOwnerTeam(UUID defaultOwnerTeamId) {
        this.defaultOwnerTeamId = defaultOwnerTeamId;
        if (this._defaultOwnerTeamRelation != null
                && (defaultOwnerTeamId == null || !defaultOwnerTeamId.equals(this._defaultOwnerTeamRelation.getId()))) {
            this._defaultOwnerTeamRelation = null;
        }
    }

    private String normalizeCategory(String rawCategory) {
        if (rawCategory == null) {
            return null;
        }
        String trimmed = rawCategory.trim();
        if (trimmed.length() > MAX_CATEGORY_LENGTH) {
            throw new DomainException(
                    CODE_PART_DEFAULT_OWNER_CATEGORY_TOO_LONG,
                    "기본 담당자 카테고리는 100자 이하여야 합니다"
            );
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void requireTarget(UUID defaultOwnerId, UUID defaultOwnerTeamId) {
        if (defaultOwnerId == null && defaultOwnerTeamId == null) {
            throw new DomainException(CODE_PART_DEFAULT_OWNER_TARGET_REQUIRED, "기본 담당자 또는 기본 담당 팀 중 하나는 필수입니다");
        }
    }
}
