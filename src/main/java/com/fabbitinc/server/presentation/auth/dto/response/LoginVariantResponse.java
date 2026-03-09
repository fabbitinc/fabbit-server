package com.fabbitinc.server.presentation.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description = "로그인 응답 변형",
        oneOf = {LoginResponse.class, ScopedLoginResponse.class}
)
public interface LoginVariantResponse {
}
