package com.fabbitinc.server.presentation.auth.dto.response;

import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.subscription.model.SeatType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record VerifyInvitationResponse(
        @Schema(description = "초대 대상 이메일", example = "member@example.com")
        String email,
        @Schema(description = "조직 이름", example = "Fabbit")
        String orgName,
        @Schema(description = "초대한 사용자 이름", example = "관리자")
        String inviterName,
        @Schema(description = "초대 역할", example = "MEMBER")
        MembershipRole role,
        @Schema(description = "예약된 좌석 타입", example = "VIEWER")
        SeatType seatType,
        @Schema(description = "기존 가입 사용자 여부", example = "false")
        boolean isExistingUser,
        @Schema(description = "초대 만료 시각")
        Instant expiresAt
) {
}
