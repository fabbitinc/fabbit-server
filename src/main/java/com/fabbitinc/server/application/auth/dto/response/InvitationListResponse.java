package com.fabbitinc.server.application.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record InvitationListResponse(
        @Schema(description = "초대 목록")
        List<InvitationResponse> invitations
) {
}
