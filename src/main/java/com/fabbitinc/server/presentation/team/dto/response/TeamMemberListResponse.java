package com.fabbitinc.server.presentation.team.dto.response;

import java.util.List;
import java.util.UUID;

public record TeamMemberListResponse(
        List<TeamMemberItemResponse> items
) {
    public record TeamMemberItemResponse(
            UUID userId,
            String fullName,
            String email,
            String phone,
            String profileImageUrl
    ) {
    }
}
