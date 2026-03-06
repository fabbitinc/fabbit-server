package com.fabbitinc.server.presentation.synthesis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "합성 작업 목록 응답")
public record SynthesisListResponse(
        @Schema(description = "합성 작업 목록")
        List<SynthesisJobResponse> items
) {
}
