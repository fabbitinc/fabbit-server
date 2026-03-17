package com.fabbitinc.server.presentation.engineeringchange.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "변경관리에 연결할 부품 초안 식별자")
public record EngineeringChangePartRevisionTargetRequest(
        @Schema(description = "리비전 ID")
        @NotNull(message = "revisionId는 필수입니다")
        UUID revisionId
) {
}
