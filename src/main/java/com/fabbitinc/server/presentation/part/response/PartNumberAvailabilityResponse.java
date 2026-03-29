package com.fabbitinc.server.presentation.part.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "품번 사용 가능 여부 응답")
public record PartNumberAvailabilityResponse(
        @Schema(description = "조회한 품번", example = "PCB-0042")
        String partNumber,

        @Schema(description = "사용 가능 여부", example = "true")
        boolean available
) {
}
