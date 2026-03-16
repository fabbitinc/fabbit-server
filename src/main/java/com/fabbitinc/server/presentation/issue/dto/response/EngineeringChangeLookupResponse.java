package com.fabbitinc.server.presentation.issue.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "변경관리 lookup 응답")
public record EngineeringChangeLookupResponse(
        List<EngineeringChangeLookupItemResponse> items
) {
}
