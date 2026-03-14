package com.fabbitinc.server.application.part.usecase.result;

import com.fabbitinc.server.domain.part.model.PartRevisionWorkflowMode;

public record UpdatePartWorkflowPolicyResult(
        PartRevisionWorkflowMode mode
) {
}
