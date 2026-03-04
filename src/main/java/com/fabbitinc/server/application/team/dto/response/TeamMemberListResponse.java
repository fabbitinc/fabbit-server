package com.fabbitinc.server.application.team.dto.response;

import java.util.List;

public record TeamMemberListResponse(
        List<TeamMemberSummaryResponse> items
) {
}
