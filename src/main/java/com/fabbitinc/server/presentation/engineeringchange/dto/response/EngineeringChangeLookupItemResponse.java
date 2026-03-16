package com.fabbitinc.server.presentation.engineeringchange.dto.response;

import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "변경관리 lookup 항목")
public record EngineeringChangeLookupItemResponse(
        UUID id,
        int number,
        String title,
        EngineeringChangeState state
) {
}
