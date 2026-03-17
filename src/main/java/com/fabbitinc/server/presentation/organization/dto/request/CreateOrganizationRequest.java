package com.fabbitinc.server.presentation.organization.dto.request;

import com.fabbitinc.server.domain.subscription.model.WorkspacePlanType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
        @Schema(description = "조직 이름", example = "Fabbit Team")
        @NotBlank(message = "org_name은 필수입니다") @Size(min = 1, max = 100, message = "org_name 길이는 1~100자여야 합니다") String orgName,

        @Schema(description = "워크스페이스 슬러그", example = "fabbit-team")
        @Size(min = 3, max = 50, message = "slug 길이는 3~50자여야 합니다") String slug,

        @Schema(description = "산업군", example = "manufacturing")
        @Size(max = 50, message = "industry 길이는 최대 50자입니다") String industry,

        @Schema(description = "팀 규모", example = "11-50")
        @Size(max = 20, message = "team_size 길이는 최대 20자입니다") String teamSize,

        @Schema(description = "플랜 유형", example = "STARTER")
        @NotNull(message = "plan_type은 필수입니다") WorkspacePlanType planType
) {
}
