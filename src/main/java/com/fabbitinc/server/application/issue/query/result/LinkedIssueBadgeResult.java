package com.fabbitinc.server.application.issue.query.result;

import com.fabbitinc.server.domain.issue.model.IssueState;
import java.util.UUID;

public record LinkedIssueBadgeResult(
        UUID id,
        int number,
        String title,
        IssueState state
) {
}
