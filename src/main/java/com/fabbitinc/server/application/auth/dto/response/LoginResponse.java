package com.fabbitinc.server.application.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
        @Schema(description = "로그인 사용자 정보")
        UserResponse user,
        @Schema(description = "로그인 토큰")
        TokenResponse tokens
) {
}
