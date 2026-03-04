package com.fabbitinc.server.application.team.dto.response;

import java.util.UUID;

public record TeamMemberSummaryResponse(
        UUID userId,
        String fullName,
        String email,
        String phone,
        String profileImageUrl
) {
}
