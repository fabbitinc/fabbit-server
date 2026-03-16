package com.fabbitinc.server.domain.issue.model;

import com.fabbitinc.server.domain.common.entity.AbstractActorAuditableEntity;
import com.fabbitinc.server.domain.common.entity.AggregateRoot;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "engineering_changes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_engineering_changes_number", columnNames = "number")
        },
        indexes = {
                @Index(name = "ix_engineering_changes_state", columnList = "state"),
                @Index(name = "ix_engineering_changes_created_by", columnList = "created_by")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EngineeringChange extends AbstractActorAuditableEntity implements AggregateRoot {

    public static final String CODE_ENGINEERING_CHANGE_ACTOR_REQUIRED = "ENGINEERING_CHANGE_ACTOR_REQUIRED";
    public static final String CODE_ENGINEERING_CHANGE_TITLE_REQUIRED = "ENGINEERING_CHANGE_TITLE_REQUIRED";
    public static final String CODE_ENGINEERING_CHANGE_TITLE_TOO_LONG = "ENGINEERING_CHANGE_TITLE_TOO_LONG";
    public static final String CODE_ENGINEERING_CHANGE_INVALID_STATE = "ENGINEERING_CHANGE_INVALID_STATE";

    private static final int MAX_TITLE_LENGTH = 500;

    @Column(name = "number", nullable = false)
    private int number;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "body", columnDefinition = "text")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private IssueState state;

    @Enumerated(EnumType.STRING)
    @Column(name = "engineering_change_state", nullable = false, length = 20)
    private EngineeringChangeState engineeringChangeState;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "merged_at")
    private Instant mergedAt;

    @Column(name = "merged_by")
    private UUID mergedBy;

    @OneToMany(mappedBy = "engineeringChange", fetch = FetchType.LAZY)
    private List<EngineeringChangeIssueLink> linkedIssues = new ArrayList<>();

    @OneToMany(mappedBy = "engineeringChange", fetch = FetchType.LAZY)
    private List<EngineeringChangeReviewer> reviewers = new ArrayList<>();

    @OneToMany(mappedBy = "engineeringChange", fetch = FetchType.LAZY)
    private List<EngineeringChangeTeamReviewer> teamReviewers = new ArrayList<>();

    @OneToMany(mappedBy = "engineeringChange", fetch = FetchType.LAZY)
    private List<EngineeringChangeComment> comments = new ArrayList<>();

    private EngineeringChange(int number, String title, String body, UUID actorId) {
        super(UuidV7Generator.next());
        this.number = number;
        this.title = requireTitle(title);
        this.body = body;
        this.state = IssueState.OPEN;
        this.engineeringChangeState = EngineeringChangeState.DRAFT;
        initializeActor(requireActorId(actorId));
    }

    public static EngineeringChange create(int number, String title, String body, UUID actorId) {
        return new EngineeringChange(number, title, body, actorId);
    }

    public void updateTitle(String title, UUID actorId) {
        UUID requiredActorId = requireActorId(actorId);
        mutate(requiredActorId, () -> this.title = requireTitle(title));
    }

    public void updateBody(String body, UUID actorId) {
        UUID requiredActorId = requireActorId(actorId);
        mutate(requiredActorId, () -> this.body = body);
    }

    public void submit(UUID actorId) {
        UUID requiredActorId = requireActorId(actorId);
        mutate(requiredActorId, () -> {
            if (engineeringChangeState != EngineeringChangeState.DRAFT) {
                throw new DomainException(
                        CODE_ENGINEERING_CHANGE_INVALID_STATE,
                        "DRAFT 상태에서만 제출할 수 있습니다"
                );
            }
            this.engineeringChangeState = EngineeringChangeState.SUBMITTED;
        });
    }

    public void merge(Instant now, UUID actorId) {
        UUID requiredActorId = requireActorId(actorId);
        mutate(requiredActorId, () -> {
            if (engineeringChangeState != EngineeringChangeState.SUBMITTED) {
                throw new DomainException(
                        CODE_ENGINEERING_CHANGE_INVALID_STATE,
                        "SUBMITTED 상태에서만 반영할 수 있습니다"
                );
            }
            this.state = IssueState.CLOSED;
            this.engineeringChangeState = EngineeringChangeState.MERGED;
            this.closedAt = now;
            this.mergedAt = now;
            this.mergedBy = requiredActorId;
        });
    }

    public void close(Instant now, UUID actorId) {
        UUID requiredActorId = requireActorId(actorId);
        mutate(requiredActorId, () -> {
            if (engineeringChangeState != EngineeringChangeState.DRAFT
                    && engineeringChangeState != EngineeringChangeState.SUBMITTED) {
                throw new DomainException(
                        CODE_ENGINEERING_CHANGE_INVALID_STATE,
                        "DRAFT 또는 SUBMITTED 상태에서만 닫을 수 있습니다"
                );
            }
            this.state = IssueState.CLOSED;
            this.engineeringChangeState = EngineeringChangeState.CLOSED;
            this.closedAt = now;
        });
    }

    public void reopen(UUID actorId) {
        UUID requiredActorId = requireActorId(actorId);
        mutate(requiredActorId, () -> {
            if (engineeringChangeState != EngineeringChangeState.CLOSED) {
                throw new DomainException(
                        CODE_ENGINEERING_CHANGE_INVALID_STATE,
                        "CLOSED 상태에서만 다시 열 수 있습니다"
                );
            }
            this.state = IssueState.OPEN;
            this.engineeringChangeState = EngineeringChangeState.SUBMITTED;
            this.closedAt = null;
        });
    }

    public EngineeringChangeIssueLink linkIssue(UUID issueId) {
        EngineeringChangeIssueLink link = EngineeringChangeIssueLink.link(this, issueId);
        linkedIssues.add(link);
        return link;
    }

    public EngineeringChangeReviewer assignReviewer(UUID userId) {
        EngineeringChangeReviewer reviewer = EngineeringChangeReviewer.assign(this, userId);
        reviewers.add(reviewer);
        return reviewer;
    }

    public EngineeringChangeTeamReviewer assignTeamReviewer(UUID teamId) {
        EngineeringChangeTeamReviewer reviewer = EngineeringChangeTeamReviewer.assign(this, teamId);
        teamReviewers.add(reviewer);
        return reviewer;
    }

    public EngineeringChangeComment writeComment(String body, UUID actorId) {
        EngineeringChangeComment comment = EngineeringChangeComment.write(this, body, actorId);
        comments.add(comment);
        return comment;
    }

    public EngineeringChangeState getEngineeringChangeState() {
        return engineeringChangeState;
    }

    public List<EngineeringChangeComment> getComments() {
        return List.copyOf(comments);
    }

    private UUID requireActorId(UUID actorId) {
        if (actorId == null) {
            throw new DomainException(CODE_ENGINEERING_CHANGE_ACTOR_REQUIRED, "수행자 ID는 필수입니다");
        }
        return actorId;
    }

    private String requireTitle(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_ENGINEERING_CHANGE_TITLE_REQUIRED, "변경 제목은 필수입니다");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_TITLE_LENGTH) {
            throw new DomainException(CODE_ENGINEERING_CHANGE_TITLE_TOO_LONG, "변경 제목은 500자 이하여야 합니다");
        }
        return trimmed;
    }
}
