package com.fabbitinc.server.application.part.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "응답 DTO")
public record PartBomResponse(
        List<BomChildResponse> children,
        List<BomParentResponse> parents
) {
}
