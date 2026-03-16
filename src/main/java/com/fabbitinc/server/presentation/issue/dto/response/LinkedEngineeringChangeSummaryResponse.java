package com.fabbitinc.server.presentation.issue.dto.response;

import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "연결 변경관리 요약")
public record LinkedEngineeringChangeSummaryResponse(
        UUID id,
        int number,
        String title,
        EngineeringChangeState state
) {
}
