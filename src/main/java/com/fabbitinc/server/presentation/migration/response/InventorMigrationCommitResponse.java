package com.fabbitinc.server.presentation.migration.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Inventor 마이그레이션 커밋 응답")
public record InventorMigrationCommitResponse(
        @Schema(description = "생성된 프로젝트 ID")
        UUID projectId,
        @Schema(description = "생성된 part ID 목록")
        List<UUID> createdPartIds,
        @Schema(description = "요약")
        SummaryResponse summary
) {
    @Schema(description = "커밋 요약")
    public record SummaryResponse(
            @Schema(description = "생성된 part 수")
            int createdPartCount,
            @Schema(description = "orphan drawing 수")
            int orphanDrawingCount
    ) {
    }
}
