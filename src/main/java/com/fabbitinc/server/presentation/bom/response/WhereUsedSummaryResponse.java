package com.fabbitinc.server.presentation.bom.response;

import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Where-used 요약 응답")
public record WhereUsedSummaryResponse(

        @Schema(description = "직접 참조 수", example = "5")
        int directReferenceCount,

        @Schema(description = "상태별 집계")
        StatusBreakdown statusBreakdown,

        @Schema(description = "참조 목록")
        List<Reference> references
) {

    @Schema(description = "리비전 상태별 집계")
    public record StatusBreakdown(

            @Schema(description = "DRAFT 수", example = "2")
            int draftCount,

            @Schema(description = "RELEASED 수", example = "1")
            int releasedCount,

            @Schema(description = "SUPERSEDED 수", example = "1")
            int supersededCount,

            @Schema(description = "CANCELED 수", example = "1")
            int canceledCount
    ) {
    }

    @Schema(description = "Where-used 참조 항목")
    public record Reference(

            @Schema(description = "부품 ID")
            UUID partId,

            @Schema(description = "부품 번호", example = "P-001")
            String partNumber,

            @Schema(description = "부품명", example = "어셈블리 A")
            String partName,

            @Schema(description = "리비전 ID")
            UUID revisionId,

            @Schema(description = "리비전 코드", example = "A")
            String revisionCode,

            @Schema(description = "리비전 상태", example = "RELEASED")
            PartRevisionStatus revisionStatus
    ) {
    }
}
