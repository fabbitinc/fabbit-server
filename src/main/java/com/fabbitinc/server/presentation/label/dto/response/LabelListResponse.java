package com.fabbitinc.server.presentation.label.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "응답 DTO")
public record LabelListResponse(
        int total,
        List<LabelResponse> items
) {
}
