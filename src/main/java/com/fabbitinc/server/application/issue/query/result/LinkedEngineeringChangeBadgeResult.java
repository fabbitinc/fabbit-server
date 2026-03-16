package com.fabbitinc.server.application.issue.query.result;

import com.fabbitinc.server.domain.issue.model.EngineeringChangeState;
import com.fabbitinc.server.domain.issue.model.IssueState;
import java.util.UUID;

public record LinkedEngineeringChangeBadgeResult(
        UUID id,
        int number,
        String title,
        IssueState state,
        EngineeringChangeState engineeringChangeState
) {
}
