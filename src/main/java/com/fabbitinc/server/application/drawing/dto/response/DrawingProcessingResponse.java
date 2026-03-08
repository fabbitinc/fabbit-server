package com.fabbitinc.server.application.drawing.dto.response;

import com.fabbitinc.server.application.drawing.query.result.DrawingProcessingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "도면 처리 상태 응답 DTO")
public record DrawingProcessingResponse(
        @Schema(description = "도면 처리 상태", example = "PENDING")
        DrawingProcessingStatus status,
        @Schema(description = "실패 사유", example = "지원하지 않는 도면 파일 형식입니다")
        String failureReason,
        @Schema(description = "PDF 산출물 준비 여부", example = "true")
        boolean pdfReady,
        @Schema(description = "WEBP 산출물 준비 여부", example = "true")
        boolean webpReady,
        @Schema(description = "GLB 산출물 준비 여부", example = "false")
        boolean glbReady
) {
}
