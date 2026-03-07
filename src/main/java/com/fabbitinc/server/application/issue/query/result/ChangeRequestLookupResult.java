package com.fabbitinc.server.application.issue.query.result;

import com.fabbitinc.server.domain.issue.model.CrState;
import com.fabbitinc.server.domain.issue.model.IssueState;
import java.util.List;
import java.util.UUID;

public record ChangeRequestLookupResult(
        List<Item> items
) {
    public record Item(
            UUID id,
            int number,
            String title,
            IssueState state,
            CrState crState
    ) {
    }
}
