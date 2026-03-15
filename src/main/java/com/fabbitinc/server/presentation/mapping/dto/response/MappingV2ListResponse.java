package com.fabbitinc.server.presentation.mapping.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "V2 매핑 목록 응답")
public record MappingV2ListResponse(
        @Schema(description = "V2 매핑 목록")
        List<MappingV2Response> items
) {
}
