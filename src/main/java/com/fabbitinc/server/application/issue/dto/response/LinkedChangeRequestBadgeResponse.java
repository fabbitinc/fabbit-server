package com.fabbitinc.server.application.issue.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "연결 변경요청 배지")
public record LinkedChangeRequestBadgeResponse(
        UUID id,
        int number,
        String title,
        String state,
        String crState
) {
}
