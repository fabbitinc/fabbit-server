package com.fabbitinc.server.application.user.query.result;

import com.fabbitinc.server.domain.organization.model.MembershipRole;

import java.util.UUID;

public record UserMembershipResult(
        UUID orgId,
        MembershipRole role,
        String jobRole,
        QueryOrganizationResult organization
) {
}
