package com.fabbitinc.server.application.project.query.result;

import java.util.List;

public record ProjectPartsResult(
        long total,
        List<ProjectPartSummaryResult> items
) {
}
