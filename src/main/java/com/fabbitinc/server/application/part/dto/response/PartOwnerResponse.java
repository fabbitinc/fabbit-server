package com.fabbitinc.server.application.part.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "응답 DTO")
public record PartOwnerResponse(
        UUID ownerId,
        PartOwnerUserSummaryResponse owner,
        UUID ownerTeamId,
        String ownerTeamName
) {
}
