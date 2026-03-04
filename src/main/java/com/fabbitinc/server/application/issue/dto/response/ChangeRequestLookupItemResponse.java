package com.fabbitinc.server.application.issue.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "변경요청 lookup 항목")
public record ChangeRequestLookupItemResponse(
        UUID id,
        int number,
        String title,
        String state,
        String crState
) {
}
