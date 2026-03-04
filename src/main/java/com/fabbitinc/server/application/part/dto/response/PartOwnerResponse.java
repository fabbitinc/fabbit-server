package com.fabbitinc.server.application.part.dto.response;

import java.util.UUID;

public record PartOwnerResponse(
        UUID ownerId,
        PartOwnerUserSummaryResponse owner,
        UUID ownerTeamId,
        String ownerTeamName
) {
}
