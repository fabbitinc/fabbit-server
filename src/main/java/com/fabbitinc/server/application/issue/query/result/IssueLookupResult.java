package com.fabbitinc.server.application.issue.query.result;

import com.fabbitinc.server.domain.issue.model.IssueState;
import com.fabbitinc.server.domain.issue.model.IssueType;
import java.util.List;
import java.util.UUID;

public record IssueLookupResult(
        List<Item> items
) {
    public record Item(
            UUID id,
            int number,
            String title,
            IssueState state,
            IssueType type
    ) {
    }
}
