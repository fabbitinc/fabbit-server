package com.fabbitinc.server.presentation.part.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "진행중 부품 목록 응답 DTO")
public record PartInProgressListResponse(
        @Schema(description = "다음 페이지 조회 기준 커서")
        String nextCursor,
        @Schema(description = "이전 페이지 조회 기준 커서")
        String prevCursor,
        @Schema(description = "목록 항목")
        List<PartInProgressItemResponse> items
) {
}
