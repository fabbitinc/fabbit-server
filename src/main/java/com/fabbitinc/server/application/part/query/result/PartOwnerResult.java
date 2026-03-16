package com.fabbitinc.server.application.part.query.result;
import com.fabbitinc.server.application.workitem.query.result.UserSummaryResult;

import java.util.UUID;

public record PartOwnerResult(
        UUID ownerId,
        PartUserSummaryResult owner,
        UUID ownerTeamId,
        String ownerTeamName
) {
}
