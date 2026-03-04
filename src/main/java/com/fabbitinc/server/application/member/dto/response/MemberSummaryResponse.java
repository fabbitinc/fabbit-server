package com.fabbitinc.server.application.member.dto.response;

import java.util.UUID;

public record MemberSummaryResponse(
        UUID userId,
        String fullName,
        String email,
        String phone,
        String profileImageUrl,
        String role,
        String jobRole
) {
}
