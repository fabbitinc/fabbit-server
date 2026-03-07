package com.fabbitinc.server.domain.issue.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "change_requests")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue("CHANGE_REQUEST")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChangeRequest extends Issue {

    @Enumerated(EnumType.STRING)
    @Column(name = "cr_state", nullable = false, length = 20)
    private CrState crState;

    @Column(name = "merged_at")
    private Instant mergedAt;

    @Column(name = "merged_by")
    private UUID mergedBy;

    @OneToMany(mappedBy = "changeRequest", fetch = FetchType.LAZY)
    private List<ChangeRequestIssue> linkedIssues = new ArrayList<>();

    @OneToMany(mappedBy = "changeRequest", fetch = FetchType.LAZY)
    private List<ChangeRequestReviewer> reviewers = new ArrayList<>();

    @OneToMany(mappedBy = "changeRequest", fetch = FetchType.LAZY)
    private List<ChangeRequestTeamReviewer> teamReviewers = new ArrayList<>();

    private ChangeRequest(int number, String title, String body, UUID actorId) {
        super(number, title, body, actorId);
        this.crState = CrState.DRAFT;
    }

    public static ChangeRequest create(int number, String title, String body, UUID actorId) {
        return new ChangeRequest(number, title, body, actorId);
    }

    public void submit(UUID actorId) {
        if (crState != CrState.DRAFT) {
            throw new DomainException(
                    CODE_ISSUE_INVALID_STATE,
                    "DRAFT 상태에서만 제출할 수 있습니다 (현재: " + crState + ")"
            );
        }
        touch(requireActorId(actorId));
        this.crState = CrState.SUBMITTED;
    }

    public void merge(Instant now, UUID actorId) {
        if (crState != CrState.SUBMITTED) {
            throw new DomainException(
                    CODE_ISSUE_INVALID_STATE,
                    "SUBMITTED 상태에서만 반영할 수 있습니다 (현재: " + crState + ")"
            );
        }
        UUID requiredActorId = requireActorId(actorId);
        markClosed(now, requiredActorId);
        this.crState = CrState.MERGED;
        this.mergedAt = now;
        this.mergedBy = requiredActorId;
    }

    public void closeCr(Instant now, UUID actorId) {
        if (crState != CrState.DRAFT && crState != CrState.SUBMITTED) {
            throw new DomainException(
                    CODE_ISSUE_INVALID_STATE,
                    "DRAFT 또는 SUBMITTED 상태에서만 닫을 수 있습니다 (현재: " + crState + ")"
            );
        }
        UUID requiredActorId = requireActorId(actorId);
        markClosed(now, requiredActorId);
        this.crState = CrState.CLOSED;
    }

    public void reopenCr(UUID actorId) {
        if (crState != CrState.CLOSED) {
            throw new DomainException(
                    CODE_ISSUE_INVALID_STATE,
                    "CLOSED 상태에서만 다시 열 수 있습니다 (현재: " + crState + ")"
            );
        }
        UUID requiredActorId = requireActorId(actorId);
        markOpen(requiredActorId);
        this.crState = CrState.SUBMITTED;
    }

    public List<ChangeRequestIssue> getLinkedIssues() {
        return List.copyOf(linkedIssues);
    }

    public List<ChangeRequestReviewer> getReviewers() {
        return List.copyOf(reviewers);
    }

    public List<ChangeRequestTeamReviewer> getTeamReviewers() {
        return List.copyOf(teamReviewers);
    }

    public ChangeRequestIssue linkIssue(UUID issueId) {
        ChangeRequestIssue link = ChangeRequestIssue.link(this, issueId);
        linkedIssues.add(link);
        return link;
    }

    public ChangeRequestReviewer assignReviewer(UUID userId) {
        ChangeRequestReviewer reviewer = ChangeRequestReviewer.assign(this, userId);
        reviewers.add(reviewer);
        return reviewer;
    }

    public ChangeRequestTeamReviewer assignTeamReviewer(UUID teamId) {
        ChangeRequestTeamReviewer reviewer = ChangeRequestTeamReviewer.assign(this, teamId);
        teamReviewers.add(reviewer);
        return reviewer;
    }

    private UUID requireActorId(UUID actorId) {
        if (actorId == null) {
            throw new DomainException(Issue.CODE_ISSUE_ACTOR_REQUIRED, "수행자 ID는 필수입니다");
        }
        return actorId;
    }
}
