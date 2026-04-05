package com.fabbitinc.server.domain.engineeringchange.model;

import com.fabbitinc.server.domain.common.entity.AbstractActorAuditableEntity;
import com.fabbitinc.server.domain.common.entity.AggregateRoot;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
@Table(name = "workflow_templates")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkflowTemplate extends AbstractActorAuditableEntity implements AggregateRoot {

    public static final String CODE_TEMPLATE_NAME_REQUIRED = "WORKFLOW_TEMPLATE_NAME_REQUIRED";
    public static final String CODE_TEMPLATE_NAME_TOO_LONG = "WORKFLOW_TEMPLATE_NAME_TOO_LONG";
    public static final String CODE_TEMPLATE_ACTOR_REQUIRED = "WORKFLOW_TEMPLATE_ACTOR_REQUIRED";

    private static final int MAX_NAME_LENGTH = 200;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @OneToMany(mappedBy = "_workflowTemplateRelation", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkflowTemplateStage> stages = new ArrayList<>();

    private WorkflowTemplate(String name, String description, UUID actorId) {
        super(UuidV7Generator.next());
        this.name = requireName(name);
        this.description = description;
        initializeActor(requireActorId(actorId));
    }

    public static WorkflowTemplate create(String name, String description, UUID actorId) {
        return new WorkflowTemplate(name, description, actorId);
    }

    public void updateName(String name, UUID actorId) {
        mutate(requireActorId(actorId), () -> this.name = requireName(name));
    }

    public void updateDescription(String description, UUID actorId) {
        mutate(requireActorId(actorId), () -> this.description = description);
    }

    public WorkflowTemplateStage addStage(
            EngineeringChangeStepType stepType,
            int sequence,
            StepStageCompletionPolicy completionPolicy,
            Integer minApprovals
    ) {
        WorkflowTemplateStage stage = WorkflowTemplateStage.create(
                this, stepType, sequence, completionPolicy, minApprovals
        );
        stages.add(stage);
        return stage;
    }

    public void clearStages() {
        stages.clear();
    }

    public List<WorkflowTemplateStage> getStages() {
        return List.copyOf(stages);
    }

    private String requireName(String value) {
        if (value == null || value.isBlank()) {
            throw new DomainException(CODE_TEMPLATE_NAME_REQUIRED, "템플릿 이름은 필수입니다");
        }
        String trimmed = value.trim();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new DomainException(CODE_TEMPLATE_NAME_TOO_LONG, "템플릿 이름은 200자 이하여야 합니다");
        }
        return trimmed;
    }

    private UUID requireActorId(UUID actorId) {
        if (actorId == null) {
            throw new DomainException(CODE_TEMPLATE_ACTOR_REQUIRED, "수행자 ID는 필수입니다");
        }
        return actorId;
    }
}
