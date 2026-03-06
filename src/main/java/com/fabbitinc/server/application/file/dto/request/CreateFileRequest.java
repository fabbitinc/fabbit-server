package com.fabbitinc.server.application.file.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "요청 DTO")
public record CreateFileRequest(
        @NotBlank(message = "original_name은 필수입니다")
        @Size(max = 500, message = "original_name 길이는 최대 500자입니다")
        String originalName,

        @NotBlank(message = "content_type은 필수입니다")
        @Size(max = 100, message = "content_type 길이는 최대 100자입니다")
        String contentType,

        @Positive(message = "file_size는 0보다 커야 합니다")
        long fileSize
) {
}
