package com.fabbitinc.server.presentation.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreateOrganizationResponse(
        @Schema(description = "생성된 조직 정보")
        OrganizationResponse organization,
        @Schema(description = "로그인 토큰")
        TokenResponse tokens
) {
}
