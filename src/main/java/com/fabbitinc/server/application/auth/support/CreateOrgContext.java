package com.fabbitinc.server.application.auth.support;

import java.util.UUID;

public record CreateOrgContext(
        UUID userId,
        String email
) {
}
