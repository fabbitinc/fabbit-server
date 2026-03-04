package com.fabbitinc.server.domain.issue.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "cr_team_reviewers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_cr_team_reviewers_cr_id_team_id",
                        columnNames = {"change_request_id", "team_id"}
                )
        },
        indexes = {
                @Index(name = "ix_cr_team_reviewers_change_request_id", columnList = "change_request_id"),
                @Index(name = "ix_cr_team_reviewers_team_id", columnList = "team_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChangeRequestTeamReviewer extends AbstractCreatedEntity {

    @Column(name = "change_request_id", nullable = false)
    private UUID changeRequestId;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    public ChangeRequestTeamReviewer(UUID changeRequestId, UUID teamId) {
        super(UuidV7Generator.next());
        this.changeRequestId = changeRequestId;
        this.teamId = teamId;
    }
}
