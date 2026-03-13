package com.fabbitinc.server.application.part.dto.response;

import com.fabbitinc.server.application.part.model.PartAttachmentType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "부품 첨부 항목")
public record PartAttachmentItemResponse(
        @Schema(description = "첨부 타입", example = "DRAWING")
        PartAttachmentType attachmentType,
        @Schema(description = "일반 첨부파일 ID")
        UUID fileId,
        @Schema(description = "도면 ID")
        UUID drawingId,
        @Schema(description = "원본 파일명")
        String originalName,
        @Schema(description = "콘텐츠 타입")
        String contentType,
        @Schema(description = "파일 크기(byte)")
        long fileSize,
        @Schema(description = "다운로드 URL")
        String fileUrl,
        @Schema(description = "미리보기 선택 가능 여부")
        boolean previewSelectable,
        @Schema(description = "현재 대표 미리보기 선택 여부")
        boolean selectedAsPreview,
        @Schema(description = "생성 시각")
        Instant createdAt
) {
}
