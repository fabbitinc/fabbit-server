package com.fabbitinc.server.domain.engineeringchange.model;

import com.fabbitinc.server.domain.common.entity.AbstractActorAuditableEntity;
import com.fabbitinc.server.domain.common.entity.AggregateRoot;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
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
    public static final String CODE_ENGINEERING_CHANGE_NO_STAGES = "ENGINEERING_CHANGE_NO_STAGES";

    private static final int MAX_TITLE_LENGTH = 500;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "number", nullable = false)
    private int number;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "body", columnDefinition = "text")
    private String body;

    @Column(name = "source_issue_id")
    private UUID sourceIssueId;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 20)
    private EngineeringChangeState state;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "released_at")
    private Instant releasedAt;

    @Column(name = "released_by")
    private UUID releasedBy;

    @OneToMany(mappedBy = "engineeringChange", fetch = FetchType.LAZY)
    private List<EngineeringChangeIssueLink> linkedIssues = new ArrayList<>();

    @OneToMany(mappedBy = "_engineeringChangeRelation", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StepStage> stages = new ArrayList<>();

    @OneToMany(mappedBy = "_engineeringChangeRelation", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EngineeringChangeStep> steps = new ArrayList<>();

    @OneToMany(mappedBy = "engineeringChange", fetch = FetchType.LAZY)
    private List<EngineeringChangeComment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "_engineeringChangeRelation", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EngineeringChangeAffectedItem> affectedItems = new ArrayList<>();

    private EngineeringChange(int number, String title, String body, UUID sourceIssueId, UUID actorId) {
        super(UuidV7Generator.next());
        this.number = number;
        this.title = requireTitle(title);
        this.body = body;
        this.sourceIssueId = sourceIssueId;
        this.state = EngineeringChangeState.DRAFT;
        initializeActor(requireActorId(actorId));
    }

    public static EngineeringChange create(int number, String title, String body, UUID sourceIssueId, UUID actorId) {
        return new EngineeringChange(number, title, body, sourceIssueId, actorId);
    }

    // ── 메타데이터 수정 ──

    public void updateTitle(String title, UUID actorId) {
        UUID requiredActorId = requireActorId(actorId);
        mutate(requiredActorId, () -> this.title = requireTitle(title));
    }

    public void updateBody(String body, UUID actorId) {
        UUID requiredActorId = requireActorId(actorId);
        mutate(requiredActorId, () -> this.body = body);
    }

    // ── Stage 관리 ──

    public StepStage addStage(
            EngineeringChangeStepType stepType,
            int sequence,
            StepStageCompletionPolicy completionPolicy,
            Integer minApprovals,
            Instant deadline,
            UUID actorId
    ) {
        UUID requiredActorId = requireActorId(actorId);
        StepStage stage = StepStage.create(this, stepType, sequence, completionPolicy, minApprovals, deadline);
        mutate(requiredActorId, () -> stages.add(stage));
        return stage;
    }

    public void clearStages(UUID actorId) {
        UUID requiredActorId = requireActorId(actorId);
        mutate(requiredActorId, () -> {
            steps.clear();
            stages.clear();
        });
    }

    public List<StepStage> getStages() {
        return List.copyOf(stages);
    }

    // ── Step 관리 (aggregate root가 step 생성 소유) ──

    public EngineeringChangeStep addStep(
            StepStage stage,
            EngineeringChangeStepAssigneeType assigneeType,
            UUID assigneeId,
            UUID actorId
    ) {
        UUID requiredActorId = requireActorId(actorId);
        EngineeringChangeStep step = EngineeringChangeStep.assign(stage, assigneeType, assigneeId);
        mutate(requiredActorId, () -> steps.add(step));
        return step;
    }

    public List<EngineeringChangeStep> getSteps() {
        return List.copyOf(steps);
    }

    // ── 상태 전이 (Step-Driven) ──

    /**
     * stage/step 상태에서 EC 상태를 도출하여 동기화한다.
     * 이 메서드가 EC 상태 변경의 단일 진입점이다.
     */
    public void syncStateFromStages(UUID actorId) {
        UUID requiredActorId = requireActorId(actorId);

        if (state == EngineeringChangeState.RELEASED || state == EngineeringChangeState.CANCELED) {
            return;
        }

        if (stages.isEmpty()) {
            transitionTo(EngineeringChangeState.DRAFT, requiredActorId);
            return;
        }

        // step에 REJECTED가 있으면 전체 리셋
        boolean hasRejected = steps.stream()
                .anyMatch(s -> s.getStatus() == EngineeringChangeStepStatus.REJECTED);
        if (hasRejected) {
            transitionTo(EngineeringChangeState.DRAFT, requiredActorId);
            return;
        }

        // 최소 sequence의 미완료 stage 찾기
        List<StepStage> sortedStages = stages.stream()
                .sorted(Comparator.comparingInt(StepStage::getSequence))
                .toList();

        for (StepStage stage : sortedStages) {
            List<EngineeringChangeStep> stageSteps = steps.stream()
                    .filter(s -> s.getStepStageId().equals(stage.getId()))
                    .toList();

            boolean allApprovedOrCanceled = stageSteps.stream()
                    .allMatch(s -> s.isApproved()
                            || s.getStatus() == EngineeringChangeStepStatus.CANCELED);

            if (!allApprovedOrCanceled) {
                // 이 stage가 아직 미완료 → stage의 stepType으로 EC 상태 결정
                EngineeringChangeState derivedState = mapStepTypeToState(stage.getStepType());
                transitionTo(derivedState, requiredActorId);
                return;
            }
        }

        // 모든 stage 완료 → RELEASE_PENDING
        transitionTo(EngineeringChangeState.RELEASE_PENDING, requiredActorId);
    }

    public void submit(UUID actorId) {
        UUID requiredActorId = requireActorId(actorId);
        if (state != EngineeringChangeState.DRAFT) {
            throw new DomainException(CODE_ENGINEERING_CHANGE_INVALID_STATE,
                    "DRAFT 상태에서만 제출할 수 있습니다");
        }
        if (stages.isEmpty()) {
            throw new DomainException(CODE_ENGINEERING_CHANGE_NO_STAGES,
                    "단계(Stage)가 최소 1개 이상 있어야 제출할 수 있습니다");
        }
        syncStateFromStages(requiredActorId);
    }

    public void release(Instant now, UUID actorId) {
        UUID requiredActorId = requireActorId(actorId);
        if (state != EngineeringChangeState.RELEASE_PENDING) {
            throw new DomainException(CODE_ENGINEERING_CHANGE_INVALID_STATE,
                    "RELEASE_PENDING 상태에서만 반영할 수 있습니다");
        }
        mutate(requiredActorId, () -> {
            this.state = EngineeringChangeState.RELEASED;
            this.closedAt = now;
            this.releasedAt = now;
            this.releasedBy = requiredActorId;
        });
    }

    public void cancel(Instant now, UUID actorId) {
        UUID requiredActorId = requireActorId(actorId);
        if (state == EngineeringChangeState.RELEASED || state == EngineeringChangeState.CANCELED) {
            throw new DomainException(CODE_ENGINEERING_CHANGE_INVALID_STATE,
                    "진행 중인 변경안만 폐기할 수 있습니다");
        }
        mutate(requiredActorId, () -> {
            this.state = EngineeringChangeState.CANCELED;
            this.closedAt = now;
        });
    }

    /**
     * 전체 step을 PENDING으로 리셋하고 EC를 DRAFT로 복귀시킨다.
     * rejected 시 호출된다.
     */
    public void resetAllSteps(UUID actorId) {
        UUID requiredActorId = requireActorId(actorId);
        mutate(requiredActorId, () -> {
            for (EngineeringChangeStep step : steps) {
                step.reset();
            }
            this.state = EngineeringChangeState.DRAFT;
        });
    }

    // ── 기타 관계 ──

    public EngineeringChangeIssueLink linkIssue(UUID issueId) {
        EngineeringChangeIssueLink link = EngineeringChangeIssueLink.link(this, issueId);
        linkedIssues.add(link);
        return link;
    }

    public EngineeringChangeComment writeComment(String body, UUID actorId) {
        EngineeringChangeComment comment = EngineeringChangeComment.write(this, body, actorId);
        comments.add(comment);
        return comment;
    }

    public List<EngineeringChangeComment> getComments() {
        return List.copyOf(comments);
    }

    public EngineeringChangeAffectedItem addAffectedItem(
            EngineeringChangeAffectedItemType itemType,
            UUID targetId,
            String actionDetail
    ) {
        EngineeringChangeAffectedItem item = EngineeringChangeAffectedItem.create(
                this.getId(), itemType, targetId, actionDetail
        );
        affectedItems.add(item);
        return item;
    }

    public void clearAffectedItems() {
        affectedItems.clear();
    }

    public List<EngineeringChangeAffectedItem> getAffectedItems() {
        return List.copyOf(affectedItems);
    }

    /**
     * CHANGES_REQUESTED 상태의 step이 존재하는지 확인한다.
     * part revision 수정 가능 여부 판단에 사용.
     */
    public boolean hasChangesRequestedStep() {
        return steps.stream().anyMatch(EngineeringChangeStep::isChangesRequested);
    }

    // ── 내부 헬퍼 ──

    private void transitionTo(EngineeringChangeState newState, UUID actorId) {
        if (this.state != newState) {
            mutate(actorId, () -> this.state = newState);
        }
    }

    private EngineeringChangeState mapStepTypeToState(EngineeringChangeStepType stepType) {
        return switch (stepType) {
            case REVIEW -> EngineeringChangeState.REVIEW_PENDING;
            case APPROVAL -> EngineeringChangeState.APPROVAL_PENDING;
            case RELEASE -> EngineeringChangeState.RELEASE_PENDING;
        };
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

    public Instant getReleasedAt() {
        return releasedAt;
    }

    public UUID getReleasedBy() {
        return releasedBy;
    }
}
