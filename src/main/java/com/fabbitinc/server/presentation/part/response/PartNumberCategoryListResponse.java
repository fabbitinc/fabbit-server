package com.fabbitinc.server.presentation.part.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "채번 카테고리 목록 응답")
public record PartNumberCategoryListResponse(
        @Schema(description = "채번 카테고리 목록")
        List<PartNumberCategoryResponse> items
) {
}
