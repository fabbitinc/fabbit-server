package com.fabbitinc.server.presentation.migration.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Inventor 마이그레이션 시작 응답")
public record InventorMigrationStartResponse(
        @Schema(description = "마이그레이션 세션 ID")
        UUID sessionId,
        @Schema(description = "프로젝트 이름")
        String projectName,
        @Schema(description = "전체 파일 수")
        int totalFileCount,
        @Schema(description = "가져오기 대상 파일 수")
        int importableFileCount,
        @Schema(description = "업로드 대상 목록")
        List<UploadTargetResponse> uploadTargets
) {
    @Schema(description = "업로드 대상")
    public record UploadTargetResponse(
            @Schema(description = "매니페스트 경로")
            String path,
            @Schema(description = "파일 ID")
            UUID fileId,
            @Schema(description = "업로드 URL")
            String uploadUrl,
            @Schema(description = "스토리지 키")
            String fileKey
    ) {
    }
}
