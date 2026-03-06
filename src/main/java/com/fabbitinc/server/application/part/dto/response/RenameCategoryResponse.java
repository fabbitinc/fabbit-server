package com.fabbitinc.server.application.part.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
@Schema(description = "응답 DTO")
public record RenameCategoryResponse(
        int updatedCount
) {
}
