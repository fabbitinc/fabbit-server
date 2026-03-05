package com.fabbitinc.server.application.user.query.result;

import java.util.UUID;

public record UserMembershipResult(
        UUID orgId,
        String role,
        String jobRole,
        QueryOrganizationResult organization
) {
}
