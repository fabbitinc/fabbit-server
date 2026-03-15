package com.fabbitinc.server.application.settings.usecase.result;

import com.fabbitinc.server.domain.part.model.PartRevisionWorkflowMode;

public record UpdateSettingsPartWorkflowPolicyResult(
        PartRevisionWorkflowMode mode
) {
}
