package com.fabbitinc.server.application.user.dto.response;

import com.fabbitinc.server.application.auth.dto.response.OrganizationResponse;

import java.util.UUID;

public record UserMembershipResponse(
        UUID orgId,
        String role,
        String jobRole,
        OrganizationResponse organization
) {
}
