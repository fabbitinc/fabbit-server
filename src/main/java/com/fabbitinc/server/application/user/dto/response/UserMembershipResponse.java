package com.fabbitinc.server.application.user.dto.response;

import com.fabbitinc.server.presentation.auth.dto.response.OrganizationResponse;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record UserMembershipResponse(
        @Schema(description = "조직 ID")
        UUID orgId,
        @Schema(description = "조직 내 권한 역할", example = "ADMIN")
        MembershipRole role,
        @Schema(description = "담당 직무(자유 입력 텍스트)", example = "백엔드 엔지니어")
        String jobRole,
        @Schema(description = "조직 정보")
        OrganizationResponse organization
) {
}
