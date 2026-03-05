package com.fabbitinc.server.application.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record ChangeRoleRequest(
        @Schema(description = "변경할 멤버 역할", example = "ADMIN")
        @NotBlank(message = "role은 필수입니다")
        String role
) {
}
