package com.fabbitinc.server.application.synthesis.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "응답 DTO")
public record SynthesisListResponse(
        List<SynthesisJobResponse> items
) {
}
