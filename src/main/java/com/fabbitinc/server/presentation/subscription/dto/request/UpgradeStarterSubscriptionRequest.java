package com.fabbitinc.server.presentation.subscription.dto.request;

import com.fabbitinc.server.domain.subscription.model.SeatType;
import com.fabbitinc.server.domain.subscription.model.WorkspacePlanType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@Schema(description = "Starter 플랜 즉시 업그레이드 요청")
public record UpgradeStarterSubscriptionRequest(
        @Schema(description = "즉시 업그레이드할 대상 플랜", example = "TEAM")
        @NotNull
        WorkspacePlanType targetPlanType,

        @Schema(description = "현재 워크스페이스 멤버 전원의 좌석 타입 지정")
        @NotEmpty
        List<@Valid MemberSeatRequest> memberSeats
) {

    @Schema(description = "멤버별 좌석 타입 지정")
    public record MemberSeatRequest(
            @Schema(description = "좌석 타입을 지정할 멤버십 ID", example = "019cf746-2095-7b80-9930-c099a25a2c7b")
            @NotNull
            UUID membershipId,

            @Schema(description = "업그레이드 후 적용할 좌석 타입", example = "FULL")
            @NotNull
            SeatType seatType
    ) {
    }
}
