package com.fabbitinc.server.application.auth.dto.response;

public record AcceptInvitationResponse(
        UserResponse user,
        OrganizationResponse organization,
        TokenResponse tokens,
        boolean isNewUser
) {
}
