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
                @Index(name = "ix_engineering_change_steps_engineering_change_id", columnList = "engineering_change_id"),
                @Index(name = "ix_engineering_change_steps_step_type", columnList = "step_type"),
                @Index(name = "ix_engineering_change_steps_assignee_id", columnList = "assignee_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EngineeringChangeStep extends AbstractCreatedEntity {

    public static final String CODE_ENGINEERING_CHANGE_STEP_REQUIRED = "ENGINEERING_CHANGE_STEP_REQUIRED";
    public static final String CODE_ENGINEERING_CHANGE_STEP_ASSIGNEE_REQUIRED =
            "ENGINEERING_CHANGE_STEP_ASSIGNEE_REQUIRED";
    public static final String CODE_ENGINEERING_CHANGE_STEP_TYPE_REQUIRED =
            "ENGINEERING_CHANGE_STEP_TYPE_REQUIRED";
    public static final String CODE_ENGINEERING_CHANGE_STEP_ASSIGNEE_TYPE_REQUIRED =
            "ENGINEERING_CHANGE_STEP_ASSIGNEE_TYPE_REQUIRED";
    public static final String CODE_ENGINEERING_CHANGE_STEP_INVALID_STATUS =
            "ENGINEERING_CHANGE_STEP_INVALID_STATUS";
    public static final String CODE_ENGINEERING_CHANGE_STEP_ACTOR_REQUIRED =
            "ENGINEERING_CHANGE_STEP_ACTOR_REQUIRED";
    public static final String CODE_ENGINEERING_CHANGE_STEP_ACTED_AT_REQUIRED =
            "ENGINEERING_CHANGE_STEP_ACTED_AT_REQUIRED";
    public static final String CODE_ENGINEERING_CHANGE_STEP_SEQUENCE_INVALID =
            "ENGINEERING_CHANGE_STEP_SEQUENCE_INVALID";

    @Column(name = "engineering_change_id", nullable = false)
    private UUID engineeringChangeId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "engineering_change_id", insertable = false, updatable = false)
    private EngineeringChange _engineeringChangeRelation;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false, length = 30)
    private EngineeringChangeStepType stepType;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignee_type", nullable = false, length = 20)
    private EngineeringChangeStepAssigneeType assigneeType;

    @Column(name = "assignee_id", nullable = false)
    private UUID assigneeId;

    @Column(name = "sequence", nullable = false)
    private int sequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EngineeringChangeStepStatus status;

    @Column(name = "acted_at")
    private Instant actedAt;

    @Column(name = "acted_by")
    private UUID actedBy;

    private EngineeringChangeStep(
            UUID engineeringChangeId,
            EngineeringChangeStepType stepType,
            EngineeringChangeStepAssigneeType assigneeType,
            UUID assigneeId,
            int sequence
    ) {
        super(UuidV7Generator.next());
        this.engineeringChangeId = requireEngineeringChangeId(engineeringChangeId);
        this.stepType = requireStepType(stepType);
        this.assigneeType = requireAssigneeType(assigneeType);
        this.assigneeId = requireAssigneeId(assigneeId);
        this.sequence = requireSequence(sequence);
        this.status = EngineeringChangeStepStatus.PENDING;
    }

    public static EngineeringChangeStep assign(
            EngineeringChange engineeringChange,
            EngineeringChangeStepType stepType,
            EngineeringChangeStepAssigneeType assigneeType,
            UUID assigneeId,
            int sequence
    ) {
        if (engineeringChange == null) {
            throw new DomainException(CODE_ENGINEERING_CHANGE_STEP_REQUIRED, "변경관리 ID는 필수입니다");
        }
        EngineeringChangeStep step = new EngineeringChangeStep(
                engineeringChange.getId(),
                stepType,
                assigneeType,
                assigneeId,
                sequence
        );
        step._engineeringChangeRelation = engineeringChange;
        return step;
    }

    public void approve(UUID actorId, Instant actedAt) {
        if (status != EngineeringChangeStepStatus.PENDING) {
            throw new DomainException(CODE_ENGINEERING_CHANGE_STEP_INVALID_STATUS, "대기 중인 단계만 승인할 수 있습니다");
        }
        this.status = EngineeringChangeStepStatus.APPROVED;
        this.actedBy = requireActorId(actorId);
        this.actedAt = requireActedAt(actedAt);
    }

    public void reject(UUID actorId, Instant actedAt) {
        if (status != EngineeringChangeStepStatus.PENDING) {
            throw new DomainException(CODE_ENGINEERING_CHANGE_STEP_INVALID_STATUS, "대기 중인 단계만 반려할 수 있습니다");
        }
        this.status = EngineeringChangeStepStatus.REJECTED;
        this.actedBy = requireActorId(actorId);
        this.actedAt = requireActedAt(actedAt);
    }

    public void reset() {
        this.status = EngineeringChangeStepStatus.PENDING;
        this.actedAt = null;
        this.actedBy = null;
    }

    public boolean isPending() {
        return status == EngineeringChangeStepStatus.PENDING;
    }

    public boolean isAssignedToUser(UUID userId) {
        return assigneeType == EngineeringChangeStepAssigneeType.USER && assigneeId.equals(userId);
    }

    public boolean isAssignedToTeam(UUID teamId) {
        return assigneeType == EngineeringChangeStepAssigneeType.TEAM && assigneeId.equals(teamId);
    }

    private UUID requireEngineeringChangeId(UUID engineeringChangeId) {
        if (engineeringChangeId == null) {
            throw new DomainException(CODE_ENGINEERING_CHANGE_STEP_REQUIRED, "변경관리 ID는 필수입니다");
        }
        return engineeringChangeId;
    }

    private EngineeringChangeStepType requireStepType(EngineeringChangeStepType stepType) {
        if (stepType == null) {
            throw new DomainException(CODE_ENGINEERING_CHANGE_STEP_TYPE_REQUIRED, "단계 타입은 필수입니다");
        }
        return stepType;
    }

    private EngineeringChangeStepAssigneeType requireAssigneeType(EngineeringChangeStepAssigneeType assigneeType) {
        if (assigneeType == null) {
            throw new DomainException(CODE_ENGINEERING_CHANGE_STEP_ASSIGNEE_TYPE_REQUIRED, "담당자 타입은 필수입니다");
        }
        return assigneeType;
    }

    private UUID requireAssigneeId(UUID assigneeId) {
        if (assigneeId == null) {
            throw new DomainException(CODE_ENGINEERING_CHANGE_STEP_ASSIGNEE_REQUIRED, "담당자 ID는 필수입니다");
        }
        return assigneeId;
    }

    private int requireSequence(int value) {
        if (value < 1) {
            throw new DomainException(CODE_ENGINEERING_CHANGE_STEP_SEQUENCE_INVALID, "단계 순서는 1 이상이어야 합니다");
        }
        return value;
    }

    private UUID requireActorId(UUID actorId) {
        if (actorId == null) {
            throw new DomainException(CODE_ENGINEERING_CHANGE_STEP_ACTOR_REQUIRED, "수행자 ID는 필수입니다");
        }
        return actorId;
    }

    private Instant requireActedAt(Instant value) {
        if (value == null) {
            throw new DomainException(CODE_ENGINEERING_CHANGE_STEP_ACTED_AT_REQUIRED, "처리 시각은 필수입니다");
        }
        return value;
    }
}
