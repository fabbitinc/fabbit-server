package com.fabbitinc.server.presentation.team.dto.response;

import java.util.List;
import java.util.UUID;

public record TeamLookupResponse(
        List<TeamLookupItemResponse> items
) {
    public record TeamLookupItemResponse(
            UUID id,
            String name
    ) {
    }
}
