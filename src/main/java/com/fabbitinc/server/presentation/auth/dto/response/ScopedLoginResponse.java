package com.fabbitinc.server.presentation.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ScopedLoginResponse(
        @Schema(description = "로그인 사용자 정보")
        UserResponse user,
        @Schema(description = "조직 생성용 scoped access token", example = "eyJhbGciOiJIUzI1NiJ9...")
        String scopedToken
) implements LoginVariantResponse {
}
