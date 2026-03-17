package com.fabbitinc.server.presentation.part.response;

import com.fabbitinc.server.application.part.query.result.PartPreviewProcessingFailureCode;
import com.fabbitinc.server.domain.part.model.PartPreviewProcessingStatus;
import com.fabbitinc.server.domain.part.model.PartPreviewSourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "부품 대표 미리보기 처리 상태 응답")
public record PartPreviewProcessingResponse(
        @Schema(description = "현재 처리 중인 대표 미리보기 소스 타입", example = "DRAWING")
        PartPreviewSourceType sourceType,
        @Schema(description = "현재 처리 중인 대표 미리보기 소스 ID")
        UUID sourceId,
        @Schema(description = "대표 미리보기 처리 상태", example = "PROCESSING")
        PartPreviewProcessingStatus status,
        @Schema(description = "대표 미리보기 처리 실패 코드", example = "TIMEOUT")
        PartPreviewProcessingFailureCode failureCode,
        @Schema(description = "대표 미리보기 처리 실패 메시지")
        String failureMessage,
        @Schema(description = "PDF 산출물 준비 여부", example = "true")
        boolean pdfReady,
        @Schema(description = "WEBP 산출물 준비 여부", example = "true")
        boolean webpReady,
        @Schema(description = "GLB 산출물 준비 여부", example = "false")
        boolean glbReady
) {
}
