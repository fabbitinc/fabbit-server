package com.fabbitinc.server.presentation.part.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "부품 대표 미리보기 선택 항목 목록")
public record PartPreviewSourcesResponse(
        long total,
        List<PartPreviewSourceItemResponse> items
) {
}
