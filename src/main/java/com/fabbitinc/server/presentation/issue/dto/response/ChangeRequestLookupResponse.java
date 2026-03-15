package com.fabbitinc.server.presentation.issue.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "변경요청 lookup 응답")
public record ChangeRequestLookupResponse(
        List<ChangeRequestLookupItemResponse> items
) {
}
