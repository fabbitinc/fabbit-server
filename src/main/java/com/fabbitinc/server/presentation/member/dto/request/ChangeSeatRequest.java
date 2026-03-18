package com.fabbitinc.server.presentation.member.dto.request;

import com.fabbitinc.server.domain.subscription.model.SeatType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ChangeSeatRequest(
        @Schema(description = "변경할 좌석 타입", example = "COLLABORATOR")
        @NotNull(message = "seatType은 필수입니다")
        SeatType seatType
) {
}
