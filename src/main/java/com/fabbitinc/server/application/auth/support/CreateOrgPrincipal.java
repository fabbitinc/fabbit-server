package com.fabbitinc.server.application.auth.support;

import java.util.UUID;

public record CreateOrgPrincipal(
        UUID userId,
        String email
) {
    public CreateOrgContext toCreateOrgContext() {
        return new CreateOrgContext(userId, email);
    }
}
