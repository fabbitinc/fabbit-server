package com.fabbitinc.server.domain.issue.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
public class EngineeringChangeTeamReviewer extends AbstractCreatedEntity {

    public static final String CODE_ENGINEERING_CHANGE_TEAM_REVIEWER_REQUIRED =
            "ENGINEERING_CHANGE_TEAM_REVIEWER_REQUIRED";
    public static final String CODE_ENGINEERING_CHANGE_TEAM_REVIEWER_TEAM_REQUIRED =
            "ENGINEERING_CHANGE_TEAM_REVIEWER_TEAM_REQUIRED";

    @Column(name = "change_request_id", nullable = false)
    private UUID engineeringChangeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "change_request_id", insertable = false, updatable = false)
    private EngineeringChange engineeringChange;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    private EngineeringChangeTeamReviewer(UUID engineeringChangeId, UUID teamId) {
        super(UuidV7Generator.next());
        this.engineeringChangeId = requireEngineeringChangeId(engineeringChangeId);
        this.teamId = requireTeamId(teamId);
    }

    public static EngineeringChangeTeamReviewer assign(UUID engineeringChangeId, UUID teamId) {
        return new EngineeringChangeTeamReviewer(engineeringChangeId, teamId);
    }

    public static EngineeringChangeTeamReviewer assign(EngineeringChange engineeringChange, UUID teamId) {
        if (engineeringChange == null) {
            throw new DomainException(CODE_ENGINEERING_CHANGE_TEAM_REVIEWER_REQUIRED, "변경관리 ID는 필수입니다");
        }
        if (teamId == null) {
            throw new DomainException(CODE_ENGINEERING_CHANGE_TEAM_REVIEWER_TEAM_REQUIRED, "팀 ID는 필수입니다");
        }
        EngineeringChangeTeamReviewer reviewer = new EngineeringChangeTeamReviewer(engineeringChange.getId(), teamId);
        reviewer.engineeringChange = engineeringChange;
        return reviewer;
    }

    private UUID requireEngineeringChangeId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_ENGINEERING_CHANGE_TEAM_REVIEWER_REQUIRED, "변경관리 ID는 필수입니다");
        }
        return value;
    }

    private UUID requireTeamId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_ENGINEERING_CHANGE_TEAM_REVIEWER_TEAM_REQUIRED, "팀 ID는 필수입니다");
        }
        return value;
    }
}
