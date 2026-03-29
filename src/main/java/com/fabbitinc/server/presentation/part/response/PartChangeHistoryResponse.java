package com.fabbitinc.server.presentation.part.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "부품 변경 이력 응답")
public record PartChangeHistoryResponse(
        @Schema(description = "변경 이력 항목 목록") List<PartChangeHistoryItemResponse> items
) {
    @Schema(description = "부품 변경 이력 항목")
    public record PartChangeHistoryItemResponse(
            @Schema(description = "시각") Instant timestamp,
            @Schema(description = "유형", example = "EC_RELEASED") String type,
            @Schema(description = "참조 ID") UUID referenceId,
            @Schema(description = "참조 번호", example = "12") int referenceNumber,
            @Schema(description = "제목", example = "부품 재질 변경") String title,
            @Schema(description = "행위자 이름", example = "홍길동") String actorName
    ) {
    }
}
