package com.fabbitinc.server.application.team.query.result;

import java.util.List;
import java.util.UUID;

public record TeamMemberListResult(
        List<TeamMemberSummaryResult> items
) {
    public record TeamMemberSummaryResult(
            UUID userId,
            String fullName,
            String email,
            String phone,
            String profileImageUrl
    ) {
    }
}
