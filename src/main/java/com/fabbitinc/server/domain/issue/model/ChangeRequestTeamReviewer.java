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

    public static final String CODE_CR_TEAM_REVIEWER_CHANGE_REQUEST_REQUIRED = "CR_TEAM_REVIEWER_CHANGE_REQUEST_REQUIRED";
    public static final String CODE_CR_TEAM_REVIEWER_TEAM_REQUIRED = "CR_TEAM_REVIEWER_TEAM_REQUIRED";

    @Column(name = "change_request_id", nullable = false)
    private UUID changeRequestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "change_request_id", insertable = false, updatable = false)
    private ChangeRequest changeRequest;

    @Column(name = "team_id", nullable = false)
    private UUID teamId;

    private ChangeRequestTeamReviewer(UUID changeRequestId, UUID teamId) {
        super(UuidV7Generator.next());
        this.changeRequestId = requireChangeRequestId(changeRequestId);
        this.teamId = requireTeamId(teamId);
    }

    public static ChangeRequestTeamReviewer assign(UUID changeRequestId, UUID teamId) {
        return new ChangeRequestTeamReviewer(changeRequestId, teamId);
    }

    public static ChangeRequestTeamReviewer assign(ChangeRequest changeRequest, UUID teamId) {
        if (changeRequest == null) {
            throw new DomainException(CODE_CR_TEAM_REVIEWER_CHANGE_REQUEST_REQUIRED, "변경요청 ID는 필수입니다");
        }
        if (teamId == null) {
            throw new DomainException(CODE_CR_TEAM_REVIEWER_TEAM_REQUIRED, "팀 ID는 필수입니다");
        }
        ChangeRequestTeamReviewer reviewer = new ChangeRequestTeamReviewer(changeRequest.getId(), teamId);
        reviewer.changeRequest = changeRequest;
        return reviewer;
    }

    private UUID requireChangeRequestId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_CR_TEAM_REVIEWER_CHANGE_REQUEST_REQUIRED, "변경요청 ID는 필수입니다");
        }
        return value;
    }

    private UUID requireTeamId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_CR_TEAM_REVIEWER_TEAM_REQUIRED, "팀 ID는 필수입니다");
        }
        return value;
    }
}
