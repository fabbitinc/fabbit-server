package com.fabbitinc.server.application.auth.dto.response;

import java.util.List;

public record InvitationListResponse(
        List<InvitationResponse> invitations
) {
}
