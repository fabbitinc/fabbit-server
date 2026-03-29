package com.fabbitinc.server.presentation.bom.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "BOM 가져오기 미리보기 응답")
public record BomImportPreviewResponse(

        @Schema(description = "행별 검증 결과 목록")
        List<RowResult> rows,

        @Schema(description = "요약 통계")
        SummaryResponse summary
) {

    @Schema(description = "행별 검증 결과")
    public record RowResult(

            @Schema(description = "엑셀 행 번호", example = "2")
            int rowNumber,

            @Schema(description = "줄 번호", example = "10")
            String lineNumber,

            @Schema(description = "하위 부품 번호", example = "PART-001")
            String childPartNumber,

            @Schema(description = "하위 리비전 코드", example = "A")
            String childRevisionCode,

            @Schema(description = "수량", example = "2.5")
            BigDecimal quantity,

            @Schema(description = "검증 상태 (SUCCESS, ERROR, WARNING)", example = "SUCCESS")
            String status,

            @Schema(description = "오류/경고 메시지", example = "부품을 찾을 수 없습니다")
            String message
    ) {
    }

    @Schema(description = "미리보기 요약 통계")
    public record SummaryResponse(

            @Schema(description = "전체 행 수", example = "10")
            int totalCount,

            @Schema(description = "성공 행 수", example = "8")
            int successCount,

            @Schema(description = "오류 행 수", example = "2")
            int errorCount,

            @Schema(description = "경고 행 수", example = "0")
            int warningCount
    ) {
    }
}
