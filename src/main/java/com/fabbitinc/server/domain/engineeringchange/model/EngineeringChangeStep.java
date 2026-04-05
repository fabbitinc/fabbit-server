package com.fabbitinc.server.domain.engineeringchange.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "engineering_change_steps",
        indexes = {
                @Index(name = "ix_ec_steps_stage_id", columnList = "step_stage_id"),
                @Index(name = "ix_ec_steps_ec_id", columnList = "engineering_change_id"),
                @Index(name = "ix_ec_steps_assignee_id", columnList = "assignee_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EngineeringChangeStep extends AbstractCreatedEntity {

    public static final String CODE_STEP_STAGE_REQUIRED = "ENGINEERING_CHANGE_STEP_STAGE_REQUIRED";
    public static final String CODE_STEP_ASSIGNEE_REQUIRED = "ENGINEERING_CHANGE_STEP_ASSIGNEE_REQUIRED";
    public static final String CODE_STEP_ASSIGNEE_TYPE_REQUIRED = "ENGINEERING_CHANGE_STEP_ASSIGNEE_TYPE_REQUIRED";
    public static final String CODE_STEP_INVALID_STATUS = "ENGINEERING_CHANGE_STEP_INVALID_STATUS";
    public static final String CODE_STEP_ACTOR_REQUIRED = "ENGINEERING_CHANGE_STEP_ACTOR_REQUIRED";
    public static final String CODE_STEP_ACTED_AT_REQUIRED = "ENGINEERING_CHANGE_STEP_ACTED_AT_REQUIRED";
    public static final String CODE_STEP_STAGE_ALREADY_COMPLETE = "ENGINEERING_CHANGE_STEP_STAGE_ALREADY_COMPLETE";

    @Column(name = "engineering_change_id", nullable = false)
    private UUID engineeringChangeId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "engineering_change_id", insertable = false, updatable = false)
    private EngineeringChange _engineeringChangeRelation;

    @Column(name = "step_stage_id", nullable = false)
    private UUID stepStageId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_stage_id", insertable = false, updatable = false)
    private StepStage _stepStageRelation;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignee_type", nullable = false, length = 20)
    private EngineeringChangeStepAssigneeType assigneeType;

    @Column(name = "assignee_id", nullable = false)
    private UUID assigneeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private EngineeringChangeStepStatus status;

    @Column(name = "acted_at")
    private Instant actedAt;

    @Column(name = "acted_by")
    private UUID actedBy;

    private EngineeringChangeStep(
            UUID engineeringChangeId,
            UUID stepStageId,
            EngineeringChangeStepAssigneeType assigneeType,
            UUID assigneeId
    ) {
        super(UuidV7Generator.next());
        this.engineeringChangeId = engineeringChangeId;
        this.stepStageId = requireStepStageId(stepStageId);
        this.assigneeType = requireAssigneeType(assigneeType);
        this.assigneeId = requireAssigneeId(assigneeId);
        this.status = EngineeringChangeStepStatus.PENDING;
    }

    public static EngineeringChangeStep assign(
            StepStage stage,
            EngineeringChangeStepAssigneeType assigneeType,
            UUID assigneeId
    ) {
        if (stage == null) {
            throw new DomainException(CODE_STEP_STAGE_REQUIRED, "단계(Stage)는 필수입니다");
        }
        EngineeringChangeStep step = new EngineeringChangeStep(
                stage.getEngineeringChangeId(), stage.getId(), assigneeType, assigneeId
        );
        step._stepStageRelation = stage;
        return step;
    }

    public void approve(UUID actorId, Instant actedAt) {
        assertStatus(EngineeringChangeStepStatus.PENDING, "대기 중인 단계만 승인할 수 있습니다");
        this.status = EngineeringChangeStepStatus.APPROVED;
        this.actedBy = requireActorId(actorId);
        this.actedAt = requireActedAt(actedAt);
    }

    public void reject(UUID actorId, Instant actedAt) {
        assertStatus(EngineeringChangeStepStatus.PENDING, "대기 중인 단계만 반려할 수 있습니다");
        this.status = EngineeringChangeStepStatus.REJECTED;
        this.actedBy = requireActorId(actorId);
        this.actedAt = requireActedAt(actedAt);
    }

    public void requestChanges(UUID actorId, Instant actedAt) {
        assertStatus(EngineeringChangeStepStatus.PENDING, "대기 중인 단계만 수정 요청할 수 있습니다");
        this.status = EngineeringChangeStepStatus.CHANGES_REQUESTED;
        this.actedBy = requireActorId(actorId);
        this.actedAt = requireActedAt(actedAt);
    }

    public void resubmit() {
        assertStatus(EngineeringChangeStepStatus.CHANGES_REQUESTED,
                "수정 요청 상태의 단계만 재제출할 수 있습니다");
        this.status = EngineeringChangeStepStatus.PENDING;
        this.actedBy = null;
        this.actedAt = null;
    }

    public void withdrawApproval() {
        assertStatus(EngineeringChangeStepStatus.APPROVED,
                "승인된 단계만 승인을 철회할 수 있습니다");
        this.status = EngineeringChangeStepStatus.PENDING;
        this.actedBy = null;
        this.actedAt = null;
    }

    public void cancel() {
        assertStatus(EngineeringChangeStepStatus.PENDING, "대기 중인 단계만 취소할 수 있습니다");
        this.status = EngineeringChangeStepStatus.CANCELED;
    }

    public void reset() {
        this.status = EngineeringChangeStepStatus.PENDING;
        this.actedAt = null;
        this.actedBy = null;
    }

    public boolean isPending() {
        return status == EngineeringChangeStepStatus.PENDING;
    }

    public boolean isApproved() {
        return status == EngineeringChangeStepStatus.APPROVED;
    }

    public boolean isChangesRequested() {
        return status == EngineeringChangeStepStatus.CHANGES_REQUESTED;
    }

    public boolean isAssignedToUser(UUID userId) {
        return assigneeType == EngineeringChangeStepAssigneeType.USER && assigneeId.equals(userId);
    }

    public boolean isAssignedToTeam(UUID teamId) {
        return assigneeType == EngineeringChangeStepAssigneeType.TEAM && assigneeId.equals(teamId);
    }

    private void assertStatus(EngineeringChangeStepStatus expected, String message) {
        if (status != expected) {
            throw new DomainException(CODE_STEP_INVALID_STATUS, message);
        }
    }

    private UUID requireStepStageId(UUID id) {
        if (id == null) {
            throw new DomainException(CODE_STEP_STAGE_REQUIRED, "단계(Stage) ID는 필수입니다");
        }
        return id;
    }

    private EngineeringChangeStepAssigneeType requireAssigneeType(EngineeringChangeStepAssigneeType type) {
        if (type == null) {
            throw new DomainException(CODE_STEP_ASSIGNEE_TYPE_REQUIRED, "담당자 타입은 필수입니다");
        }
        return type;
    }

    private UUID requireAssigneeId(UUID id) {
        if (id == null) {
            throw new DomainException(CODE_STEP_ASSIGNEE_REQUIRED, "담당자 ID는 필수입니다");
        }
        return id;
    }

    private UUID requireActorId(UUID actorId) {
        if (actorId == null) {
            throw new DomainException(CODE_STEP_ACTOR_REQUIRED, "수행자 ID는 필수입니다");
        }
        return actorId;
    }

    private Instant requireActedAt(Instant value) {
        if (value == null) {
            throw new DomainException(CODE_STEP_ACTED_AT_REQUIRED, "처리 시각은 필수입니다");
        }
        return value;
    }
}
