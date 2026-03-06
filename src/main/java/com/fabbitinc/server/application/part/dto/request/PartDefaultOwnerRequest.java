package com.fabbitinc.server.application.part.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "요청 DTO")
public record PartDefaultOwnerRequest(
        String category,
        UUID defaultOwnerId,
        UUID defaultOwnerTeamId
) {
}
