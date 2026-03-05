package com.fabbitinc.server.application.user.dto.response;

import com.fabbitinc.server.application.auth.dto.response.OrganizationResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record UserMembershipResponse(
        @Schema(description = "조직 ID")
        UUID orgId,
        @Schema(description = "조직 내 내 역할", example = "ADMIN")
        String role,
        @Schema(description = "직무 역할", example = "ENGINEER")
        String jobRole,
        @Schema(description = "조직 정보")
        OrganizationResponse organization
) {
}
