package com.fabbitinc.server.application.member.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ChangeRoleRequest(
        @NotBlank(message = "role은 필수입니다")
        String role
) {
}
