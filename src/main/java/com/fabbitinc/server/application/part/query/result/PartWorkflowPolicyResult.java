package com.fabbitinc.server.application.part.query.result;

import com.fabbitinc.server.domain.part.model.PartRevisionWorkflowMode;

public record PartWorkflowPolicyResult(
        PartRevisionWorkflowMode mode
) {
}
