package com.fabbitinc.server.application.usage.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "응답 DTO")
public record StorageTrendResponse(
        List<StorageTrendItem> items
) {
}
