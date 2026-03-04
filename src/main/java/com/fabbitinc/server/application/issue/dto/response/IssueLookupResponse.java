package com.fabbitinc.server.application.issue.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "이슈 lookup 응답")
public record IssueLookupResponse(
        List<IssueLookupItemResponse> items
) {
}
