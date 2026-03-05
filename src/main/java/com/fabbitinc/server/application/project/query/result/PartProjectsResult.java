package com.fabbitinc.server.application.project.query.result;

import java.util.List;

public record PartProjectsResult(
        long total,
        List<PartProjectSummaryResult> items
) {
}
