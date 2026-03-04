package com.fabbitinc.server.application.user.dto.response;

import com.fabbitinc.server.application.auth.dto.response.UserResponse;

import java.util.List;

public record MeResponse(
        UserResponse user,
        List<UserMembershipResponse> memberships
) {
}
