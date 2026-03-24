package com.fabbitinc.server.presentation.part.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "부품 대표 미리보기 전용 파일 등록 요청")
public record UploadPartPreviewFileRequest(
        @Schema(description = "업로드 완료된 파일 ID")
        @NotNull(message = "file_id는 필수입니다") UUID fileId
) {
}
