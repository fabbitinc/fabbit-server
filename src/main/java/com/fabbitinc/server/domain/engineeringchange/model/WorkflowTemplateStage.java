package com.fabbitinc.server.domain.engineeringchange.model;

import com.fabbitinc.server.domain.common.entity.AbstractCreatedEntity;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.CascadeType;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "workflow_template_stages",
        indexes = {
                @Index(name = "ix_wf_tpl_stages_template_id", columnList = "workflow_template_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkflowTemplateStage extends AbstractCreatedEntity {

    public static final String CODE_TEMPLATE_STAGE_REQUIRED = "WF_TEMPLATE_STAGE_TEMPLATE_REQUIRED";
    public static final String CODE_TEMPLATE_STAGE_STEP_TYPE_REQUIRED = "WF_TEMPLATE_STAGE_STEP_TYPE_REQUIRED";
    public static final String CODE_TEMPLATE_STAGE_POLICY_REQUIRED = "WF_TEMPLATE_STAGE_POLICY_REQUIRED";
    public static final String CODE_TEMPLATE_STAGE_SEQUENCE_INVALID = "WF_TEMPLATE_STAGE_SEQUENCE_INVALID";
    public static final String CODE_TEMPLATE_STAGE_MIN_APPROVALS_REQUIRED = "WF_TEMPLATE_STAGE_MIN_APPROVALS_REQUIRED";

    @Column(name = "workflow_template_id", nullable = false)
    private UUID workflowTemplateId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_template_id", insertable = false, updatable = false)
    private WorkflowTemplate _workflowTemplateRelation;

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

    @OneToMany(mappedBy = "_workflowTemplateStageRelation", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkflowTemplateStageAssignee> assignees = new ArrayList<>();

    private WorkflowTemplateStage(
            UUID workflowTemplateId,
            EngineeringChangeStepType stepType,
            int sequence,
            StepStageCompletionPolicy completionPolicy,
            Integer minApprovals
    ) {
        super(UuidV7Generator.next());
        this.workflowTemplateId = workflowTemplateId;
        this.stepType = requireStepType(stepType);
        this.sequence = requireSequence(sequence);
        this.completionPolicy = requirePolicy(completionPolicy);
        this.minApprovals = validateMinApprovals(completionPolicy, minApprovals);
    }

    static WorkflowTemplateStage create(
            WorkflowTemplate template,
            EngineeringChangeStepType stepType,
            int sequence,
            StepStageCompletionPolicy completionPolicy,
            Integer minApprovals
    ) {
        if (template == null) {
            throw new DomainException(CODE_TEMPLATE_STAGE_REQUIRED, "템플릿은 필수입니다");
        }
        WorkflowTemplateStage stage = new WorkflowTemplateStage(
                template.getId(), stepType, sequence, completionPolicy, minApprovals
        );
        stage._workflowTemplateRelation = template;
        return stage;
    }

    public WorkflowTemplateStageAssignee addAssignee(
            EngineeringChangeStepAssigneeType assigneeType,
            UUID assigneeId
    ) {
        WorkflowTemplateStageAssignee assignee = WorkflowTemplateStageAssignee.create(
                this, assigneeType, assigneeId
        );
        assignees.add(assignee);
        return assignee;
    }

    public List<WorkflowTemplateStageAssignee> getAssignees() {
        return List.copyOf(assignees);
    }

    private EngineeringChangeStepType requireStepType(EngineeringChangeStepType value) {
        if (value == null) {
            throw new DomainException(CODE_TEMPLATE_STAGE_STEP_TYPE_REQUIRED, "단계 타입은 필수입니다");
        }
        return value;
    }

    private StepStageCompletionPolicy requirePolicy(StepStageCompletionPolicy value) {
        if (value == null) {
            throw new DomainException(CODE_TEMPLATE_STAGE_POLICY_REQUIRED, "완료 정책은 필수입니다");
        }
        return value;
    }

    private int requireSequence(int value) {
        if (value < 1) {
            throw new DomainException(CODE_TEMPLATE_STAGE_SEQUENCE_INVALID, "순서는 1 이상이어야 합니다");
        }
        return value;
    }

    private Integer validateMinApprovals(StepStageCompletionPolicy policy, Integer minApprovals) {
        if (policy == StepStageCompletionPolicy.MIN_N_APPROVES && (minApprovals == null || minApprovals < 1)) {
            throw new DomainException(CODE_TEMPLATE_STAGE_MIN_APPROVALS_REQUIRED,
                    "MIN_N_APPROVES 정책에서는 최소 승인 수(1 이상)가 필수입니다");
        }
        return minApprovals;
    }
}
