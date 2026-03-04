package com.fabbitinc.server.application.project.dto.response;

import java.util.UUID;

public record ProjectMemberSummaryResponse(
        UUID userId,
        String fullName,
        String email,
        String phone,
        String profileImageUrl,
        String role
) {
}
