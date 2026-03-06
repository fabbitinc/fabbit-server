package com.fabbitinc.server.application.dashboard.query.result;

public record DashboardStatsResult(
        DashboardPartStatsResult parts,
        DashboardBomStatsResult bomLinks,
        DashboardLastSynthesisResult lastSynthesis
) {
}
