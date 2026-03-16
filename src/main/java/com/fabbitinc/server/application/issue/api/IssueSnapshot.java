package com.fabbitinc.server.application.issue.api;

import com.fabbitinc.server.domain.issue.model.IssueState;
import java.util.UUID;

public record IssueSnapshot(
        UUID id,
        int number,
        String title,
        IssueState state
) {
}
