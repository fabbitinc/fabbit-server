package com.fabbitinc.server.domain.team.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
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
        name = "team_members",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_team_members_team_id_user_id",
                        columnNames = {"team_id", "user_id"}
                )
        },
        indexes = {
                @Index(name = "ix_team_members_team_id", columnList = "team_id"),
                @Index(name = "ix_team_members_user_id", columnList = "user_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamMember extends AbstractCreatedEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "team_id", nullable = false, foreignKey = @ForeignKey(name = "fk_team_members_team_id"))
    private Team team;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    public TeamMember(Team team, UUID userId) {
        super(UuidV7Generator.next());
        this.team = team;
        this.userId = userId;
    }

    public UUID getTeamId() {
        return team.getId();
    }
}
