package com.fabbitinc.server.application.auth.support;

import com.fabbitinc.server.domain.organization.model.MembershipRole;
import java.util.UUID;

public record AuthPrincipal(
        UUID userId,
        String email,
        UUID orgId,
        MembershipRole role
) {
    public AuthContext toAuthContext() {
        return new AuthContext(userId, email, orgId, role);
    }
}
