package com.fabbitinc.server.presentation.subscription.dto.request;

import com.fabbitinc.server.domain.subscription.model.SeatType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "구독 좌석 수량 변경 요청")
public record UpdateSubscriptionSeatQuotasRequest(
        @Schema(description = "변경할 좌석 수량 목록")
        @NotEmpty(message = "seat_quotas는 최소 1개 이상이어야 합니다")
        List<@Valid SeatQuantityRequest> seatQuotas
) {
    public record SeatQuantityRequest(
            @Schema(
                    description = "좌석 타입, 유료 플랜 좌석만 수량 변경 가능",
                    example = "FULL",
                    allowableValues = {"VIEWER", "COLLABORATOR", "FULL"}
            )
            @NotNull(message = "seat_type은 필수입니다")
            SeatType seatType,
            @Schema(description = "구매 좌석 수량", example = "3")
            @Min(value = 0, message = "purchased_quantity는 0 이상이어야 합니다")
            int purchasedQuantity
    ) {
    }
}
