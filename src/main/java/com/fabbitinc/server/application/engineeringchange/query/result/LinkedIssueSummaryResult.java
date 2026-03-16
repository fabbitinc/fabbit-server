package com.fabbitinc.server.application.engineeringchange.query.result;

import com.fabbitinc.server.domain.issue.model.IssueState;
import java.util.UUID;

public record LinkedIssueSummaryResult(
        UUID id,
        int number,
        String title,
        IssueState state
) {
}
