package com.fabbitinc.server.presentation.mapping.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "매핑 목록 응답")
public record MappingListResponse(
        @Schema(description = "매핑 목록")
        List<MappingResponse> items
) {
}
