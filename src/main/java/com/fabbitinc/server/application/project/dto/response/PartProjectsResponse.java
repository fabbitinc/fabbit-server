package com.fabbitinc.server.application.project.dto.response;

import java.util.List;

public record PartProjectsResponse(
        long total,
        List<PartProjectSummaryResponse> items
) {
}
