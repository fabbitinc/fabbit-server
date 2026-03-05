package com.fabbitinc.server.application.member.dto.request;

import com.fabbitinc.server.domain.organization.model.MembershipRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(
        @Schema(description = "변경할 멤버 역할", example = "ADMIN")
        @NotNull(message = "role은 필수입니다")
        MembershipRole role
) {
}
