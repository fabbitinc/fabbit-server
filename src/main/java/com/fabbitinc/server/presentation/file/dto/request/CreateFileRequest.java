package com.fabbitinc.server.presentation.file.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "요청 DTO")
public record CreateFileRequest(
        @Schema(description = "원본 파일명", example = "sample.pdf")
        @NotBlank(message = "original_name은 필수입니다") @Size(max = 500, message = "original_name 길이는 최대 500자입니다") String originalName,

        @Schema(description = "콘텐츠 타입", example = "application/pdf")
        @NotBlank(message = "content_type은 필수입니다") @Size(max = 100, message = "content_type 길이는 최대 100자입니다") String contentType,

        @Schema(description = "파일 크기(bytes)", example = "1024")
        @Positive(message = "file_size는 0보다 커야 합니다") long fileSize,

        @Schema(description = "클라이언트가 계산한 SHA-256 hex", example = "6d2bc3f13b59bf38368ffce5aa7498479f880c6da14961fb1bc696ff44e43173")
        @NotBlank(message = "content_hash는 필수입니다")
        @Pattern(regexp = "^[0-9A-Fa-f]{64}$", message = "content_hash는 SHA-256 hex 형식이어야 합니다")
        String contentHash
) {
}
