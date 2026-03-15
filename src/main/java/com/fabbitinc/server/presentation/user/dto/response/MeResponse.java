package com.fabbitinc.server.presentation.user.dto.response;

import com.fabbitinc.server.presentation.auth.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record MeResponse(
        @Schema(description = "내 사용자 정보")
        UserResponse user,
        @Schema(description = "내 조직 소속 목록")
        List<UserMembershipResponse> memberships
) {
}
