package com.fabbitinc.server.application.part.dto.response;

import java.util.UUID;

public record PartDefaultOwnerItemResponse(
        UUID id,
        String category,
        UUID defaultOwnerId,
        PartOwnerUserSummaryResponse defaultOwner,
        UUID defaultOwnerTeamId,
        String defaultOwnerTeamName
) {
}
