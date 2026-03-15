package com.fabbitinc.server.application.settings.usecase.command;

import com.fabbitinc.server.domain.part.model.PartRevisionWorkflowMode;

public record UpdateSettingsPartWorkflowPolicyCommand(
        PartRevisionWorkflowMode mode
) {
}
