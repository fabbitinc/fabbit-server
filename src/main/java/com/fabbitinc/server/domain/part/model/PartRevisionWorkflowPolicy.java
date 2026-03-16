package com.fabbitinc.server.domain.part.model;

import com.fabbitinc.server.domain.common.entity.AbstractAuditableEntity;
import com.fabbitinc.server.domain.common.entity.AggregateRoot;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.common.id.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "part_revision_workflow_policies",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_part_revision_workflow_policies_policy_key",
                        columnNames = "policy_key"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartRevisionWorkflowPolicy extends AbstractAuditableEntity implements AggregateRoot {

    public static final String DEFAULT_POLICY_KEY = "DEFAULT";
    public static final String CODE_PART_REVISION_WORKFLOW_MODE_REQUIRED = "PART_REVISION_WORKFLOW_MODE_REQUIRED";

    @Column(name = "policy_key", nullable = false, updatable = false, length = 50)
    private String policyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 50)
    private PartRevisionWorkflowMode mode;

    private PartRevisionWorkflowPolicy(PartRevisionWorkflowMode mode) {
        super(UuidV7Generator.next());
        this.policyKey = DEFAULT_POLICY_KEY;
        this.mode = requireMode(mode);
    }

    public static PartRevisionWorkflowPolicy createDefault() {
        return new PartRevisionWorkflowPolicy(PartRevisionWorkflowMode.DIRECT);
    }

    public static PartRevisionWorkflowPolicy create(PartRevisionWorkflowMode mode) {
        return new PartRevisionWorkflowPolicy(mode);
    }

    public void changeMode(PartRevisionWorkflowMode mode) {
        this.mode = requireMode(mode);
    }

    public boolean requiresEngineeringChange() {
        return mode == PartRevisionWorkflowMode.CHANGE_REQUEST_REQUIRED;
    }

    private PartRevisionWorkflowMode requireMode(PartRevisionWorkflowMode value) {
        if (value == null) {
            throw new DomainException(
                    CODE_PART_REVISION_WORKFLOW_MODE_REQUIRED,
                    "리비전 워크플로 모드는 필수입니다"
            );
        }
        return value;
    }
}
