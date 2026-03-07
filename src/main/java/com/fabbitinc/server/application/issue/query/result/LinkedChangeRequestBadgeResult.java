package com.fabbitinc.server.application.issue.query.result;

import com.fabbitinc.server.domain.issue.model.CrState;
import com.fabbitinc.server.domain.issue.model.IssueState;
import java.util.UUID;

public record LinkedChangeRequestBadgeResult(
        UUID id,
        int number,
        String title,
        IssueState state,
        CrState crState
) {
}
