package com.fabbitinc.server.presentation.engineeringchange.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "변경관리 라벨 배지")
public record LabelBadgeResponse(
        UUID id,
        String name,
        String color
) {
}
