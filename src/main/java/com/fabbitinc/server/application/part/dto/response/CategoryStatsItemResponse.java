package com.fabbitinc.server.application.part.dto.response;

public record CategoryStatsItemResponse(
        String category,
        long partCount
) {
}
