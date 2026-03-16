package com.fabbitinc.server.domain.engineeringchange.model;

import com.fabbitinc.server.domain.common.entity.AbstractActorAuditableEntity;
import com.fabbitinc.server.domain.common.entity.AggregateRoot;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
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
    private EngineeringChangeState state;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "merged_at")
    private Instant mergedAt;

    @Column(name = "merged_by")
    private UUID mergedBy;

    @OneToMany(mappedBy = "engineeringChange", fetch = FetchType.LAZY)
    private List<EngineeringChangeIssueLink> linkedIssues = new ArrayList<>();

    @OneToMany(mappedBy = "_engineeringChangeRelation", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EngineeringChangeStep> steps = new ArrayList<>();

    @OneToMany(mappedBy = "engineeringChange", fetch = FetchType.LAZY)
    private List<EngineeringChangeComment> comments = new ArrayList<>();

    private EngineeringChange(int number, String title, String body, UUID actorId) {
        super(UuidV7Generator.next());
        this.number = number;
        this.title = requireTitle(title);
        this.body = body;
        this.state = EngineeringChangeState.DRAFT;
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
            if (state != EngineeringChangeState.DRAFT) {
                throw new DomainException(
                        CODE_ENGINEERING_CHANGE_INVALID_STATE,
                        "DRAFT 상태에서만 제출할 수 있습니다"
                );
            }
            this.state = EngineeringChangeState.REVIEW_PENDING;
        });
    }

    public void completeReview(UUID actorId) {
        UUID requiredActorId = requireActorId(actorId);
        mutate(requiredActorId, () -> {
            if (state != EngineeringChangeState.REVIEW_PENDING) {
                throw new DomainException(
                        CODE_ENGINEERING_CHANGE_INVALID_STATE,
                        "REVIEW_PENDING 상태에서만 승인 대기로 전환할 수 있습니다"
                );
            }
            this.state = EngineeringChangeState.APPROVAL_PENDING;
        });
    }

    public void approve(UUID actorId) {
        UUID requiredActorId = requireActorId(actorId);
        mutate(requiredActorId, () -> {
            if (state != EngineeringChangeState.APPROVAL_PENDING) {
                throw new DomainException(
                        CODE_ENGINEERING_CHANGE_INVALID_STATE,
                        "APPROVAL_PENDING 상태에서만 승인할 수 있습니다"
                );
            }
            this.state = EngineeringChangeState.RELEASE_PENDING;
        });
    }

    public void reject(UUID actorId) {
        UUID requiredActorId = requireActorId(actorId);
        mutate(requiredActorId, () -> {
            if (state != EngineeringChangeState.REVIEW_PENDING
                    && state != EngineeringChangeState.APPROVAL_PENDING
                    && state != EngineeringChangeState.RELEASE_PENDING) {
                throw new DomainException(
                        CODE_ENGINEERING_CHANGE_INVALID_STATE,
                        "대기 상태에서만 반려할 수 있습니다"
                );
            }
            this.state = EngineeringChangeState.DRAFT;
        });
    }

    public void release(Instant now, UUID actorId) {
        UUID requiredActorId = requireActorId(actorId);
        mutate(requiredActorId, () -> {
            if (state != EngineeringChangeState.RELEASE_PENDING) {
                throw new DomainException(
                        CODE_ENGINEERING_CHANGE_INVALID_STATE,
                        "RELEASE_PENDING 상태에서만 반영할 수 있습니다"
                );
            }
            this.state = EngineeringChangeState.RELEASED;
            this.closedAt = now;
            this.mergedAt = now;
            this.mergedBy = requiredActorId;
        });
    }

    public void cancel(Instant now, UUID actorId) {
        UUID requiredActorId = requireActorId(actorId);
        mutate(requiredActorId, () -> {
            if (state != EngineeringChangeState.DRAFT
                    && state != EngineeringChangeState.REVIEW_PENDING
                    && state != EngineeringChangeState.APPROVAL_PENDING
                    && state != EngineeringChangeState.RELEASE_PENDING) {
                throw new DomainException(
                        CODE_ENGINEERING_CHANGE_INVALID_STATE,
                        "진행 중인 변경안만 폐기할 수 있습니다"
                );
            }
            this.state = EngineeringChangeState.CANCELED;
            this.closedAt = now;
        });
    }

    public EngineeringChangeIssueLink linkIssue(UUID issueId) {
        EngineeringChangeIssueLink link = EngineeringChangeIssueLink.link(this, issueId);
        linkedIssues.add(link);
        return link;
    }

    public EngineeringChangeStep addStep(
            EngineeringChangeStepType stepType,
            EngineeringChangeStepAssigneeType assigneeType,
            UUID assigneeId,
            int sequence,
            UUID actorId
    ) {
        UUID requiredActorId = requireActorId(actorId);
        EngineeringChangeStep step = EngineeringChangeStep.assign(this, stepType, assigneeType, assigneeId, sequence);
        mutate(requiredActorId, () -> steps.add(step));
        return step;
    }

    public void clearSteps(UUID actorId) {
        UUID requiredActorId = requireActorId(actorId);
        mutate(requiredActorId, steps::clear);
    }

    public List<EngineeringChangeStep> getSteps() {
        return List.copyOf(steps);
    }

    public EngineeringChangeComment writeComment(String body, UUID actorId) {
        EngineeringChangeComment comment = EngineeringChangeComment.write(this, body, actorId);
        comments.add(comment);
        return comment;
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

    public int getNumber() {
        return number;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public Instant getMergedAt() {
        return mergedAt;
    }

    public UUID getMergedBy() {
        return mergedBy;
    }
}
