package com.fabbitinc.server.presentation.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record AcceptInvitationResponse(
        @Schema(description = "초대 수락 사용자 정보")
        UserResponse user,
        @Schema(description = "초대된 조직 정보")
        OrganizationResponse organization,
        @Schema(description = "로그인 토큰")
        TokenResponse tokens,
        @Schema(description = "신규 가입 사용자 여부", example = "true")
        boolean isNewUser
) {
}
