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
import jakarta.persistence.OneToMany;
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
@Table(
        name = "engineering_change_step_stages",
        indexes = {
                @Index(name = "ix_ec_step_stages_ec_id_seq", columnList = "engineering_change_id, sequence")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StepStage extends AbstractCreatedEntity {

    public static final String CODE_STEP_STAGE_EC_REQUIRED = "STEP_STAGE_EC_REQUIRED";
    public static final String CODE_STEP_STAGE_STEP_TYPE_REQUIRED = "STEP_STAGE_STEP_TYPE_REQUIRED";
    public static final String CODE_STEP_STAGE_POLICY_REQUIRED = "STEP_STAGE_POLICY_REQUIRED";
    public static final String CODE_STEP_STAGE_SEQUENCE_INVALID = "STEP_STAGE_SEQUENCE_INVALID";
    public static final String CODE_STEP_STAGE_MIN_APPROVALS_REQUIRED = "STEP_STAGE_MIN_APPROVALS_REQUIRED";
    public static final String CODE_STEP_STAGE_MIN_APPROVALS_INVALID = "STEP_STAGE_MIN_APPROVALS_INVALID";

    @Column(name = "engineering_change_id", nullable = false)
    private UUID engineeringChangeId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "engineering_change_id", insertable = false, updatable = false)
    private EngineeringChange _engineeringChangeRelation;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false, length = 30)
    private EngineeringChangeStepType stepType;

    @Column(name = "sequence", nullable = false)
    private int sequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_policy", nullable = false, length = 30)
    private StepStageCompletionPolicy completionPolicy;

    @Column(name = "min_approvals")
    private Integer minApprovals;

    @Column(name = "deadline")
    private Instant deadline;

    @OneToMany(mappedBy = "_stepStageRelation", fetch = FetchType.LAZY)
    private List<EngineeringChangeStep> steps = new ArrayList<>();

    private StepStage(
            UUID engineeringChangeId,
            EngineeringChangeStepType stepType,
            int sequence,
            StepStageCompletionPolicy completionPolicy,
            Integer minApprovals,
            Instant deadline
    ) {
        super(UuidV7Generator.next());
        this.engineeringChangeId = requireEngineeringChangeId(engineeringChangeId);
        this.stepType = requireStepType(stepType);
        this.sequence = requireSequence(sequence);
        this.completionPolicy = requireCompletionPolicy(completionPolicy);
        this.minApprovals = validateMinApprovals(completionPolicy, minApprovals);
        this.deadline = deadline;
    }

    public static StepStage create(
            EngineeringChange engineeringChange,
            EngineeringChangeStepType stepType,
            int sequence,
            StepStageCompletionPolicy completionPolicy,
            Integer minApprovals,
            Instant deadline
    ) {
        if (engineeringChange == null) {
            throw new DomainException(CODE_STEP_STAGE_EC_REQUIRED, "변경관리는 필수입니다");
        }
        StepStage stage = new StepStage(
                engineeringChange.getId(), stepType, sequence, completionPolicy, minApprovals, deadline
        );
        stage._engineeringChangeRelation = engineeringChange;
        return stage;
    }

    public List<EngineeringChangeStep> getSteps() {
        return List.copyOf(steps);
    }

    public void reconfigure(
            EngineeringChangeStepType stepType,
            int sequence,
            StepStageCompletionPolicy completionPolicy,
            Integer minApprovals,
            Instant deadline
    ) {
        this.stepType = requireStepType(stepType);
        this.sequence = requireSequence(sequence);
        this.completionPolicy = requireCompletionPolicy(completionPolicy);
        this.minApprovals = validateMinApprovals(completionPolicy, minApprovals);
        this.deadline = deadline;
    }

    private UUID requireEngineeringChangeId(UUID id) {
        if (id == null) {
            throw new DomainException(CODE_STEP_STAGE_EC_REQUIRED, "변경관리 ID는 필수입니다");
        }
        return id;
    }

    private EngineeringChangeStepType requireStepType(EngineeringChangeStepType stepType) {
        if (stepType == null) {
            throw new DomainException(CODE_STEP_STAGE_STEP_TYPE_REQUIRED, "단계 타입은 필수입니다");
        }
        return stepType;
    }

    private StepStageCompletionPolicy requireCompletionPolicy(StepStageCompletionPolicy policy) {
        if (policy == null) {
            throw new DomainException(CODE_STEP_STAGE_POLICY_REQUIRED, "완료 정책은 필수입니다");
        }
        return policy;
    }

    private int requireSequence(int value) {
        if (value < 1) {
            throw new DomainException(CODE_STEP_STAGE_SEQUENCE_INVALID, "단계 순서는 1 이상이어야 합니다");
        }
        return value;
    }

    private Integer validateMinApprovals(StepStageCompletionPolicy policy, Integer minApprovals) {
        if (policy == StepStageCompletionPolicy.MIN_N_APPROVES) {
            if (minApprovals == null) {
                throw new DomainException(CODE_STEP_STAGE_MIN_APPROVALS_REQUIRED,
                        "MIN_N_APPROVES 정책에서는 최소 승인 수가 필수입니다");
            }
            if (minApprovals < 1) {
                throw new DomainException(CODE_STEP_STAGE_MIN_APPROVALS_INVALID,
                        "최소 승인 수는 1 이상이어야 합니다");
            }
        }
        return minApprovals;
    }
}
