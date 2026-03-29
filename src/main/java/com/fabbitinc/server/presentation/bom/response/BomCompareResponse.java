package com.fabbitinc.server.presentation.bom.response;

import com.fabbitinc.server.application.part.model.PartRevisionDiffChangeType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "BOM 비교 응답")
public record BomCompareResponse(

        @Schema(description = "변경 목록")
        List<Change> changes,

        @Schema(description = "변경 요약")
        Summary summary
) {

    @Schema(description = "BOM 비교 변경 항목")
    public record Change(

            @Schema(description = "BOM 줄 번호", example = "10")
            String lineNumber,

            @Schema(description = "변경 유형", example = "ADDED")
            PartRevisionDiffChangeType changeType,

            @Schema(description = "소스 부품 번호", example = "P-001")
            String sourcePartNumber,

            @Schema(description = "소스 부품명", example = "볼트 M6")
            String sourceName,

            @Schema(description = "소스 수량", example = "4")
            BigDecimal sourceQuantity,

            @Schema(description = "대상 부품 번호", example = "P-002")
            String targetPartNumber,

            @Schema(description = "대상 부품명", example = "볼트 M8")
            String targetName,

            @Schema(description = "대상 수량", example = "6")
            BigDecimal targetQuantity
    ) {
    }

    @Schema(description = "BOM 비교 변경 요약")
    public record Summary(

            @Schema(description = "추가된 항목 수", example = "3")
            int addedCount,

            @Schema(description = "삭제된 항목 수", example = "1")
            int removedCount,

            @Schema(description = "변경된 항목 수", example = "2")
            int changedCount,

            @Schema(description = "변경 없는 항목 수", example = "10")
            int unchangedCount,

            @Schema(description = "전체 항목 수", example = "16")
            int totalCount
    ) {
    }
}
