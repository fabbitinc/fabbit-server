package com.fabbitinc.server.application.project.query.result;

import java.util.List;

public record ProjectListResult(
        long total,
        int offset,
        int limit,
        List<ProjectSummaryResult> items
) {
}
