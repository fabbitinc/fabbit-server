package com.fabbitinc.server.presentation.auth.dto.response;

import com.fabbitinc.server.domain.auth.model.InvitationStatus;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

public record InvitationResponse(
        @Schema(description = "초대 ID")
        UUID id,
        @Schema(description = "조직 ID")
        UUID orgId,
        @Schema(description = "초대 이메일")
        String email,
        @Schema(description = "초대 역할")
        MembershipRole role,
        @Schema(description = "초대 상태")
        InvitationStatus status,
        @Schema(description = "초대한 사용자 ID")
        UUID invitedBy,
        @Schema(description = "초대 만료 시각")
        Instant expiresAt,
        @Schema(description = "초대 수락 시각")
        Instant acceptedAt,
        @Schema(description = "초대 생성 시각")
        Instant createdAt
) {
}
