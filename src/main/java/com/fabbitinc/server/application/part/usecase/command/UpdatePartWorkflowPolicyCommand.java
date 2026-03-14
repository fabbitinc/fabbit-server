package com.fabbitinc.server.application.part.usecase.command;

import com.fabbitinc.server.domain.part.model.PartRevisionWorkflowMode;

public record UpdatePartWorkflowPolicyCommand(
        PartRevisionWorkflowMode mode
) {
}
