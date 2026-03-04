package com.fabbitinc.server.application.dashboard.dto.response;

public record PartStatsResponse(
        int total,
        int addedThisWeek
) {
}
