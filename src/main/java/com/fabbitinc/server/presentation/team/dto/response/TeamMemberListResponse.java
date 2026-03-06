package com.fabbitinc.server.presentation.team.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "응답 DTO")
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
