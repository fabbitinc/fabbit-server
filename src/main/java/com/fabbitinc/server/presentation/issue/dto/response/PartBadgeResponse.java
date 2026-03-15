package com.fabbitinc.server.presentation.issue.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "부품 배지")
public record PartBadgeResponse(
        UUID id,
        String partNumber,
        String name
) {
}
