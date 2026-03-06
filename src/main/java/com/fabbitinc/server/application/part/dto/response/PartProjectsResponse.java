package com.fabbitinc.server.application.part.dto.response;

import java.util.List;

public record PartProjectsResponse(
        long total,
        List<PartProjectSummaryResponse> items
) {
}
