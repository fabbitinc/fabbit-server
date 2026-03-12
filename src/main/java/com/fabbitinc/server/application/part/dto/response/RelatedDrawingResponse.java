package com.fabbitinc.server.application.part.dto.response;

import com.fabbitinc.server.application.part.model.DrawingViewerType;
import com.fabbitinc.server.domain.drawing.model.DrawingConversionStatus;
import com.fabbitinc.server.domain.drawing.model.DrawingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "관련 도면 응답")
public record RelatedDrawingResponse(
        @Schema(description = "도면 ID")
        UUID id,
        @Schema(description = "도면 번호")
        String drawingNumber,
        @Schema(description = "도면명")
        String name,
        @Schema(description = "버전")
        String version,
        @Schema(description = "도면 상태")
        DrawingStatus status,
        @Schema(description = "도면 변환 상태")
        DrawingConversionStatus conversionStatus,
        @Schema(description = "뷰어 타입", example = "PDF")
        DrawingViewerType viewerType,
        @Schema(description = "뷰어 본문 URL")
        String viewerUrl,
        @Schema(description = "미리보기 이미지 URL")
        String previewUrl,
        @Schema(description = "원본 파일 URL")
        String originalFileUrl,
        @Schema(description = "추가 업로드 가능한 render source 확장자 목록", example = "[\"pdf\", \"dxf\"]")
        List<String> allowedRenderSourceExtensions
) {
}
