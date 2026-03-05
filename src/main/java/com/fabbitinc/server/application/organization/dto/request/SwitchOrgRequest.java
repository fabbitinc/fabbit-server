package com.fabbitinc.server.application.organization.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SwitchOrgRequest(
        @Schema(description = "전환할 워크스페이스 슬러그", example = "fabbit-team")
        @NotBlank(message = "slug는 필수입니다")
        @Size(min = 1, max = 50, message = "slug 길이는 1~50자여야 합니다")
        String slug
) {
}
