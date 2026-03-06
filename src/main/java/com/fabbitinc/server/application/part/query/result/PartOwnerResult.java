package com.fabbitinc.server.application.part.query.result;

import java.util.UUID;

public record PartOwnerResult(
        UUID ownerId,
        PartUserSummaryResult owner,
        UUID ownerTeamId,
        String ownerTeamName
) {
}
