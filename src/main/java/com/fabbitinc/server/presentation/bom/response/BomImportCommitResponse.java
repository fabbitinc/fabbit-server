package com.fabbitinc.server.presentation.bom.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "BOM 가져오기 확정 응답")
public record BomImportCommitResponse(

        @Schema(description = "생성된 BOM 항목 ID 목록")
        List<UUID> createdBomItemIds,

        @Schema(description = "확정 요약")
        SummaryResponse summary
) {

    @Schema(description = "확정 요약 정보")
    public record SummaryResponse(

            @Schema(description = "생성된 항목 수", example = "10")
            int totalCreated,

            @Schema(description = "가져오기 모드", example = "APPEND")
            String mode
    ) {
    }
}
