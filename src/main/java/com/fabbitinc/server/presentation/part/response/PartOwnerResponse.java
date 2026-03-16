package com.fabbitinc.server.presentation.part.response;
import com.fabbitinc.server.presentation.workitem.dto.response.UserSummaryResponse;

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
