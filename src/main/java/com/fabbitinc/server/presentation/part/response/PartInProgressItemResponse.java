package com.fabbitinc.server.presentation.part.response;

import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "진행중 부품 항목 응답 DTO")
public record PartInProgressItemResponse(
        @Schema(description = "부품 ID")
        UUID partId,
        @Schema(description = "리비전 ID")
        UUID revisionId,
        @Schema(description = "품번")
        String partNumber,
        @Schema(description = "품명")
        String name,
        @Schema(description = "카테고리")
        String category,
        @Schema(description = "리비전 상태")
        PartRevisionStatus status,
        @Schema(description = "공식 리비전 코드")
        String revisionCode,
        @Schema(description = "기준 리비전 코드")
        String baseRevisionCode,
        @Schema(description = "라이프사이클 상태")
        PartLifecycleState lifecycleState,
        @Schema(description = "도면 첨부 여부")
        boolean hasDrawing,
        @Schema(description = "자식 BOM 개수")
        long childrenCount,
        @Schema(description = "마지막 수정 시각")
        Instant updatedAt
) {
}
