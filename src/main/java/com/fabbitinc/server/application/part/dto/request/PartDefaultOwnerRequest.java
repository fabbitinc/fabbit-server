package com.fabbitinc.server.application.part.dto.request;

import java.util.UUID;

public record PartDefaultOwnerRequest(
        String category,
        UUID defaultOwnerId,
        UUID defaultOwnerTeamId
) {
}
