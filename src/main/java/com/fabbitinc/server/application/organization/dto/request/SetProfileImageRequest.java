package com.fabbitinc.server.application.organization.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SetProfileImageRequest(
        @Schema(description = "조직 프로필로 설정할 파일 ID", example = "018f6c5f-6f80-7b8e-b4d6-6f50766d3a26")
        @NotNull(message = "file_id는 필수입니다") UUID fileId
) {
}
