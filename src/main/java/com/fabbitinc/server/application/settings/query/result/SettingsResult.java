package com.fabbitinc.server.application.settings.query.result;

import com.fabbitinc.server.domain.part.model.PartRevisionWorkflowMode;

public record SettingsResult(
        PartRevisionWorkflowMode partWorkflowMode
) {
}
