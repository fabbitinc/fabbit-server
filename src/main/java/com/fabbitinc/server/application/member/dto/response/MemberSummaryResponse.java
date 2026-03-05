package com.fabbitinc.server.application.member.dto.response;

import com.fabbitinc.server.domain.organization.model.MembershipRole;

import java.util.UUID;

public record MemberSummaryResponse(
        UUID userId,
        String fullName,
        String email,
        String phone,
        String profileImageUrl,
        MembershipRole role,
        String jobRole
) {
}
