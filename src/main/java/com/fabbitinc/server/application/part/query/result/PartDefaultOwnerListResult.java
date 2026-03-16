package com.fabbitinc.server.application.part.query.result;
import com.fabbitinc.server.application.workitem.query.result.UserSummaryResult;

import java.util.List;
import java.util.UUID;

public record PartDefaultOwnerListResult(
        List<Item> items
) {
    public record Item(
            UUID id,
            String category,
            UUID defaultOwnerId,
            PartUserSummaryResult defaultOwner,
            UUID defaultOwnerTeamId,
            String defaultOwnerTeamName
    ) {
    }
}
