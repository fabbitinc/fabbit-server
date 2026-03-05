package com.fabbitinc.server.application.member.query.result;

import java.util.UUID;

public record MemberSummaryResult(
        UUID userId,
        String fullName,
        String email,
        String phone,
        String profileImageUrl,
        String role,
        String jobRole
) {
}
