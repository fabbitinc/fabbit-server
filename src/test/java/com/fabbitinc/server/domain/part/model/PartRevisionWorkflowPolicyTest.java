package com.fabbitinc.server.domain.part.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fabbitinc.server.domain.common.exception.DomainException;
import org.junit.jupiter.api.Test;

class PartRevisionWorkflowPolicyTest {

    @Test
    void createDefault_직접승인모드로_생성한다() {
        PartRevisionWorkflowPolicy policy = PartRevisionWorkflowPolicy.createDefault();

        assertEquals(PartRevisionWorkflowPolicy.DEFAULT_POLICY_KEY, policy.getPolicyKey());
        assertEquals(PartRevisionWorkflowMode.DIRECT, policy.getMode());
    }

    @Test
    void changeMode_워크플로모드를_변경한다() {
        PartRevisionWorkflowPolicy policy = PartRevisionWorkflowPolicy.createDefault();

        policy.changeMode(PartRevisionWorkflowMode.ENGINEERING_CHANGE_REQUIRED);

        assertEquals(PartRevisionWorkflowMode.ENGINEERING_CHANGE_REQUIRED, policy.getMode());
    }

    @Test
    void create_모드가_없으면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> PartRevisionWorkflowPolicy.create(null));

        assertEquals(PartRevisionWorkflowPolicy.CODE_PART_REVISION_WORKFLOW_MODE_REQUIRED, ex.getDomainCode());
    }
}
