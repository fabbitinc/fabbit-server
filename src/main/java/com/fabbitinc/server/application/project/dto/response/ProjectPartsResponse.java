package com.fabbitinc.server.application.project.dto.response;

import java.util.List;

public record ProjectPartsResponse(
        long total,
        List<ProjectPartSummaryResponse> items
) {
}
