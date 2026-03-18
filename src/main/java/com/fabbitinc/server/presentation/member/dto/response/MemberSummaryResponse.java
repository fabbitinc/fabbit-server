package com.fabbitinc.server.presentation.member.dto.response;

import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.subscription.model.SeatType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "조직 멤버 요약 정보")
public record MemberSummaryResponse(
        @Schema(description = "사용자 ID")
        UUID userId,
        @Schema(description = "이름", example = "홍길동")
        String fullName,
        @Schema(description = "이메일", example = "user@example.com")
        String email,
        @Schema(description = "전화번호", example = "010-1234-5678")
        String phone,
        @Schema(description = "프로필 이미지 URL", example = "https://cdn.example.com/profile.png")
        String profileImageUrl,
        @Schema(description = "조직 역할", example = "ADMIN")
        MembershipRole role,
        @Schema(description = "직무 역할", example = "백엔드 엔지니어")
        String jobRole,
        @Schema(description = "현재 좌석 타입", example = "FULL")
        SeatType seatType
) {
}
