package com.fabbitinc.server.application.issue.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "팀 배지")
public record TeamBadgeResponse(
        UUID id,
        String name
) {
}
