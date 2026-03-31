package com.fabbitinc.server.presentation.migration.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Inventor 마이그레이션 미리보기 응답")
public record InventorMigrationPreviewResponse(
        @Schema(description = "마이그레이션 세션 ID")
        UUID sessionId,
        @Schema(description = "프로젝트 이름")
        String projectName,
        @Schema(description = "요약")
        SummaryResponse summary,
        @Schema(description = "가져오기 항목 목록")
        List<ItemResponse> items,
        @Schema(description = "orphan drawing 목록")
        List<OrphanDrawingResponse> orphanDrawings,
        @Schema(description = "커밋 가능 여부")
        boolean readyToCommit
) {
    @Schema(description = "미리보기 요약")
    public record SummaryResponse(
            @Schema(description = "전체 파일 수")
            int totalFileCount,
            @Schema(description = "가져오기 대상 파일 수")
            int importableFileCount,
            @Schema(description = "정상 준비된 항목 수")
            int readyItemCount,
            @Schema(description = "경고 수")
            int warningCount,
            @Schema(description = "오류 수")
            int errorCount
    ) {
    }

    @Schema(description = "가져오기 항목")
    public record ItemResponse(
            @Schema(description = "매니페스트 경로")
            String path,
            @Schema(description = "파일 타입")
            String fileType,
            @Schema(description = "도출된 partNumber")
            String derivedPartNumber,
            @Schema(description = "모델 파일 ID")
            UUID modelFileId,
            @Schema(description = "업로드 완료 여부")
            boolean uploaded,
            @Schema(description = "항목 상태", example = "READY")
            String status,
            @Schema(description = "설명 메시지")
            String message,
            @Schema(description = "매칭된 도면 파일 ID 목록")
            List<UUID> drawingFileIds,
            @Schema(description = "매칭된 도면 경로 목록")
            List<String> drawingPaths
    ) {
    }

    @Schema(description = "orphan drawing 항목")
    public record OrphanDrawingResponse(
            @Schema(description = "매니페스트 경로")
            String path,
            @Schema(description = "파일 ID")
            UUID fileId,
            @Schema(description = "업로드 완료 여부")
            boolean uploaded,
            @Schema(description = "설명 메시지")
            String message
    ) {
    }
}
