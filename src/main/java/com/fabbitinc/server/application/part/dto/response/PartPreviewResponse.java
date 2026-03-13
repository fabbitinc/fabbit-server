package com.fabbitinc.server.application.part.dto.response;

import com.fabbitinc.server.application.part.model.DrawingViewerType;
import com.fabbitinc.server.domain.drawing.model.DrawingConversionStatus;
import com.fabbitinc.server.domain.part.model.PartPreviewSourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "부품 대표 미리보기 응답")
public record PartPreviewResponse(
        @Schema(description = "대표 미리보기 ID")
        UUID id,
        @Schema(description = "대표 미리보기 소스 타입", example = "DRAWING")
        PartPreviewSourceType sourceType,
        @Schema(description = "대표 미리보기 소스 ID")
        UUID sourceId,
        @Schema(description = "미리보기 변환 상태")
        DrawingConversionStatus conversionStatus,
        @Schema(description = "뷰어 타입", example = "PDF")
        DrawingViewerType viewerType,
        @Schema(description = "뷰어 본문 URL")
        String viewerUrl,
        @Schema(description = "미리보기 이미지 URL")
        String previewUrl,
        @Schema(description = "원본 파일 URL")
        String originalFileUrl
) {
}
