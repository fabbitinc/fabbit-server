package com.fabbitinc.server.application.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateInvitationRequest(
        @Schema(description = "초대 대상 이메일", example = "member@example.com")
        @NotBlank(message = "email은 필수입니다")
        @Email(message = "유효한 이메일 형식이 아닙니다")
        String email,
        @Schema(description = "부여할 역할", example = "MEMBER")
        String role
) {
}
