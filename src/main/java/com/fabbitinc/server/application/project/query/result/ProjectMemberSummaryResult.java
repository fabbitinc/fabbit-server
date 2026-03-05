package com.fabbitinc.server.application.project.query.result;

import java.util.UUID;

public record ProjectMemberSummaryResult(
        UUID userId,
        String fullName,
        String email,
        String phone,
        String profileImageUrl,
        String role
) {
}
