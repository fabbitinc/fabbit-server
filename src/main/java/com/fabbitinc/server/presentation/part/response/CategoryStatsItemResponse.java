package com.fabbitinc.server.presentation.part.response;

import io.swagger.v3.oas.annotations.media.Schema;
@Schema(description = "응답 DTO")
public record CategoryStatsItemResponse(
        String category,
        long partCount
) {
}
