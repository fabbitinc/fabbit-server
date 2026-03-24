package com.fabbitinc.server.application.project.query.result;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProjectActivityListResult(
        List<ProjectActivityResult> items,
        UUID nextCursor,
        Map<String, ProjectActivityUserSummaryResult> users
) {
}
