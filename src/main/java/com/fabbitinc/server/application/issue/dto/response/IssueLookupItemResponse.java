package com.fabbitinc.server.application.issue.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "이슈 lookup 항목")
public record IssueLookupItemResponse(
        UUID id,
        int number,
        String title,
        String state,
        String type
) {
}
