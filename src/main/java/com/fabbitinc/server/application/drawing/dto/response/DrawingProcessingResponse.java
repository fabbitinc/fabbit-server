package com.fabbitinc.server.application.drawing.dto.response;

import com.fabbitinc.server.application.drawing.query.result.DrawingProcessingFailureCode;
import com.fabbitinc.server.application.drawing.query.result.DrawingProcessingStatus;
import com.fabbitinc.server.domain.drawing.model.DrawingActionRequiredReason;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "도면 처리 상태 응답 DTO")
public record DrawingProcessingResponse(
        @Schema(description = "도면 처리 상태", example = "PENDING")
        DrawingProcessingStatus status,
        @Schema(description = "도면 처리 실패 코드", example = "TIMEOUT")
        DrawingProcessingFailureCode failureCode,
        @Schema(description = "도면 처리 실패 메시지", example = "도면 변환 시간이 초과되었습니다.")
        String failureMessage,
        @Schema(description = "PDF 산출물 준비 여부", example = "true")
        boolean pdfReady,
        @Schema(description = "WEBP 산출물 준비 여부", example = "true")
        boolean webpReady,
        @Schema(description = "GLB 산출물 준비 여부", example = "false")
        boolean glbReady,
        @Schema(description = "추가 사용자 조치 사유", example = "RENDER_SOURCE_REQUIRED")
        DrawingActionRequiredReason actionRequiredReason,
        @Schema(description = "추가 업로드 가능한 render source 확장자 목록", example = "[\"pdf\", \"dxf\"]")
        List<String> allowedRenderSourceExtensions
) {
}
