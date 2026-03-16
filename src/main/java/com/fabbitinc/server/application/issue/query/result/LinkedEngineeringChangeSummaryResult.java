package com.fabbitinc.server.application.issue.query.result;

import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeState;
import java.util.UUID;

public record LinkedEngineeringChangeSummaryResult(
        UUID id,
        int number,
        String title,
        EngineeringChangeState state
) {
}
