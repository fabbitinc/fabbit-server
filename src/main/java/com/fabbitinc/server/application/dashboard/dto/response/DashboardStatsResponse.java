package com.fabbitinc.server.application.dashboard.dto.response;

public record DashboardStatsResponse(
        PartStatsResponse parts,
        BomStatsResponse bomLinks,
        LastSynthesisResponse lastSynthesis
) {
}
