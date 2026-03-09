package com.fabbitinc.server.presentation.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptInvitationRequest(
        @Schema(description = "초대 토큰", example = "invitation-token")
        @NotBlank(message = "token은 필수입니다") String token,

        @Schema(description = "신규 사용자일 때 사용할 비밀번호", example = "StrongPass123!")
        @Size(min = 8, max = 128, message = "password 길이는 8~128자여야 합니다") String password,

        @Schema(description = "신규 사용자일 때 사용할 이름", example = "홍길동")
        @Size(min = 1, max = 100, message = "full_name 길이는 1~100자여야 합니다") String fullName
) {
}
