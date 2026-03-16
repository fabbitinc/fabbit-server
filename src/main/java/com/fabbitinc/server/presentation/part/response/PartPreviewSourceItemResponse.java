package com.fabbitinc.server.presentation.part.response;

import com.fabbitinc.server.application.part.model.PartAttachmentType;
import com.fabbitinc.server.domain.part.model.PartPreviewSourceType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "부품 대표 미리보기 선택 항목")
public record PartPreviewSourceItemResponse(
        @Schema(description = "첨부 타입", example = "DRAWING")
        PartAttachmentType attachmentType,
        @Schema(description = "대표 미리보기 소스 타입", example = "DRAWING")
        PartPreviewSourceType sourceType,
        @Schema(description = "대표 미리보기 소스 ID. DRAWING이면 drawingId, PREVIEW_FILE이면 previewFileId입니다")
        UUID sourceId,
        @Schema(description = "미리보기 원본 파일 ID")
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
        @Schema(description = "현재 대표 미리보기 선택 여부")
        boolean selected,
        @Schema(description = "삭제 가능 여부")
        boolean deletable,
        @Schema(description = "생성 시각")
        Instant createdAt
) {
}
