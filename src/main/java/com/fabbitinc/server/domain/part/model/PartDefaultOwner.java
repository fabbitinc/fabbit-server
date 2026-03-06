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

    private static final int MAX_CATEGORY_LENGTH = 100;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "default_owner_id")
    private UUID defaultOwnerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_owner_id", insertable = false, updatable = false)
    private User defaultOwner;

    @Column(name = "default_owner_team_id")
    private UUID defaultOwnerTeamId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_owner_team_id", insertable = false, updatable = false)
    private Team defaultOwnerTeam;

    private PartDefaultOwner(String category, UUID defaultOwnerId, UUID defaultOwnerTeamId) {
        super(UuidV7Generator.next());
        this.category = normalizeCategory(category);
        this.defaultOwnerId = defaultOwnerId;
        this.defaultOwnerTeamId = defaultOwnerTeamId;
    }

    public static PartDefaultOwner create(String category, UUID defaultOwnerId, UUID defaultOwnerTeamId) {
        return new PartDefaultOwner(category, defaultOwnerId, defaultOwnerTeamId);
    }

    public static PartDefaultOwner create(String category, User defaultOwner, Team defaultOwnerTeam) {
        PartDefaultOwner row = new PartDefaultOwner(
                category,
                defaultOwner == null ? null : defaultOwner.getId(),
                defaultOwnerTeam == null ? null : defaultOwnerTeam.getId()
        );
        row.defaultOwner = defaultOwner;
        row.defaultOwnerTeam = defaultOwnerTeam;
        return row;
    }

    public void update(UUID defaultOwnerId, UUID defaultOwnerTeamId) {
        assignDefaultOwner(defaultOwnerId);
        assignDefaultOwnerTeam(defaultOwnerTeamId);
    }

    public void update(User defaultOwner, Team defaultOwnerTeam) {
        assignDefaultOwner(defaultOwner);
        assignDefaultOwnerTeam(defaultOwnerTeam);
    }

    public void assignDefaultOwner(UUID defaultOwnerId) {
        this.defaultOwnerId = defaultOwnerId;
        if (this.defaultOwner != null && (defaultOwnerId == null || !defaultOwnerId.equals(this.defaultOwner.getId()))) {
            this.defaultOwner = null;
        }
    }

    public void assignDefaultOwner(User defaultOwner) {
        this.defaultOwner = defaultOwner;
        this.defaultOwnerId = defaultOwner == null ? null : defaultOwner.getId();
    }

    public void assignDefaultOwnerTeam(UUID defaultOwnerTeamId) {
        this.defaultOwnerTeamId = defaultOwnerTeamId;
        if (this.defaultOwnerTeam != null
                && (defaultOwnerTeamId == null || !defaultOwnerTeamId.equals(this.defaultOwnerTeam.getId()))) {
            this.defaultOwnerTeam = null;
        }
    }

    public void assignDefaultOwnerTeam(Team defaultOwnerTeam) {
        this.defaultOwnerTeam = defaultOwnerTeam;
        this.defaultOwnerTeamId = defaultOwnerTeam == null ? null : defaultOwnerTeam.getId();
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
}
