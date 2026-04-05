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
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "workflow_template_stage_assignees",
        indexes = {
                @Index(name = "ix_wf_tpl_stage_assignees_stage_id", columnList = "workflow_template_stage_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkflowTemplateStageAssignee extends AbstractCreatedEntity {

    public static final String CODE_ASSIGNEE_STAGE_REQUIRED = "WF_TEMPLATE_ASSIGNEE_STAGE_REQUIRED";
    public static final String CODE_ASSIGNEE_TYPE_REQUIRED = "WF_TEMPLATE_ASSIGNEE_TYPE_REQUIRED";
    public static final String CODE_ASSIGNEE_ID_REQUIRED = "WF_TEMPLATE_ASSIGNEE_ID_REQUIRED";

    @Column(name = "workflow_template_stage_id", nullable = false)
    private UUID workflowTemplateStageId;

    @Getter(AccessLevel.NONE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_template_stage_id", insertable = false, updatable = false)
    private WorkflowTemplateStage _workflowTemplateStageRelation;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignee_type", nullable = false, length = 20)
    private EngineeringChangeStepAssigneeType assigneeType;

    @Column(name = "assignee_id", nullable = false)
    private UUID assigneeId;

    private WorkflowTemplateStageAssignee(
            UUID workflowTemplateStageId,
            EngineeringChangeStepAssigneeType assigneeType,
            UUID assigneeId
    ) {
        super(UuidV7Generator.next());
        this.workflowTemplateStageId = workflowTemplateStageId;
        this.assigneeType = requireAssigneeType(assigneeType);
        this.assigneeId = requireAssigneeId(assigneeId);
    }

    static WorkflowTemplateStageAssignee create(
            WorkflowTemplateStage stage,
            EngineeringChangeStepAssigneeType assigneeType,
            UUID assigneeId
    ) {
        if (stage == null) {
            throw new DomainException(CODE_ASSIGNEE_STAGE_REQUIRED, "템플릿 단계는 필수입니다");
        }
        WorkflowTemplateStageAssignee assignee = new WorkflowTemplateStageAssignee(
                stage.getId(), assigneeType, assigneeId
        );
        assignee._workflowTemplateStageRelation = stage;
        return assignee;
    }

    private EngineeringChangeStepAssigneeType requireAssigneeType(EngineeringChangeStepAssigneeType value) {
        if (value == null) {
            throw new DomainException(CODE_ASSIGNEE_TYPE_REQUIRED, "담당자 타입은 필수입니다");
        }
        return value;
    }

    private UUID requireAssigneeId(UUID value) {
        if (value == null) {
            throw new DomainException(CODE_ASSIGNEE_ID_REQUIRED, "담당자 ID는 필수입니다");
        }
        return value;
    }
}
