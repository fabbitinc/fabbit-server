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
        @Schema(
                description = "즉시 업그레이드할 대상 플랜, 현재 Team 또는 Organization만 지원",
                example = "TEAM",
                allowableValues = {"TEAM", "ORGANIZATION"}
        )
        @NotNull WorkspacePlanType targetPlanType,

        @Schema(description = "현재 워크스페이스 멤버 전원의 좌석 타입 지정")
        @NotEmpty List<@Valid MemberSeatRequest> memberSeats
) {

    @Schema(description = "멤버별 좌석 타입 지정")
    public record MemberSeatRequest(
            @Schema(description = "좌석 타입을 지정할 멤버십 ID", example = "019cf746-2095-7b80-9930-c099a25a2c7b")
            @NotNull UUID membershipId,

            @Schema(
                    description = "업그레이드 후 적용할 좌석 타입, 유료 플랜에서는 STARTER 좌석을 사용할 수 없음",
                    example = "FULL",
                    allowableValues = {"VIEWER", "COLLABORATOR", "FULL"}
            )
            @NotNull SeatType seatType
    ) {
    }
}
