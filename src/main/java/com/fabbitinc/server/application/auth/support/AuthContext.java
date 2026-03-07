package com.fabbitinc.server.application.auth.support;

import com.fabbitinc.server.domain.organization.model.MembershipRole;
import java.util.UUID;

public record AuthContext(
        UUID userId,
        String email,
        UUID orgId,
        MembershipRole role
) {
}
