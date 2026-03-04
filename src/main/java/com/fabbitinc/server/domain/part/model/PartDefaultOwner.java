package com.fabbitinc.server.domain.part.model;

import com.fabbitinc.server.domain.common.entity.AbstractAuditableEntity;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "default_owner_id")
    private UUID defaultOwnerId;

    @Column(name = "default_owner_team_id")
    private UUID defaultOwnerTeamId;

    public PartDefaultOwner(String category, UUID defaultOwnerId, UUID defaultOwnerTeamId) {
        super(UuidV7Generator.next());
        this.category = category;
        this.defaultOwnerId = defaultOwnerId;
        this.defaultOwnerTeamId = defaultOwnerTeamId;
    }

    public void update(UUID defaultOwnerId, UUID defaultOwnerTeamId) {
        this.defaultOwnerId = defaultOwnerId;
        this.defaultOwnerTeamId = defaultOwnerTeamId;
    }
}
