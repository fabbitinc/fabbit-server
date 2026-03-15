package com.fabbitinc.server.presentation.issue.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "사용자 요약")
public record IssueUserSummaryResponse(
        UUID userId,
        String fullName,
        String email,
        String phone,
        String profileImageUrl
) {
}
