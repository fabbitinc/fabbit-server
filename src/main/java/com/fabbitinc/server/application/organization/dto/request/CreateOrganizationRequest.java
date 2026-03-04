package com.fabbitinc.server.application.organization.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
        @NotBlank(message = "org_name은 필수입니다")
        @Size(min = 1, max = 100, message = "org_name 길이는 1~100자여야 합니다")
        String orgName,

        @Size(min = 3, max = 50, message = "slug 길이는 3~50자여야 합니다")
        String slug,

        @Size(max = 50, message = "industry 길이는 최대 50자입니다")
        String industry,

        @Size(max = 20, message = "team_size 길이는 최대 20자입니다")
        String teamSize,

        String planType
) {
}
