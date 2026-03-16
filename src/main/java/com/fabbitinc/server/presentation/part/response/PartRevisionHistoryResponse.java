package com.fabbitinc.server.presentation.part.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "리비전 이력 목록 응답")
public record PartRevisionHistoryResponse(
        List<PartRevisionHistoryItemResponse> items
) {
}
