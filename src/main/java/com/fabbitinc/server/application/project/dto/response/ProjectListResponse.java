package com.fabbitinc.server.application.project.dto.response;

import java.util.List;

public record ProjectListResponse(
        long total,
        int offset,
        int limit,
        List<ProjectSummaryResponse> items
) {
}
