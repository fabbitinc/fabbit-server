package com.fabbitinc.server.application.team.dto.response;

import java.util.UUID;

public record TeamLookupItemResponse(
        UUID id,
        String name
) {
}
