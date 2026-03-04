package com.fabbitinc.server.application.part.dto.response;

import java.util.List;

public record PartBomResponse(
        List<BomChildResponse> children,
        List<BomParentResponse> parents
) {
}
