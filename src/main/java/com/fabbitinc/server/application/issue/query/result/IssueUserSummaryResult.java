package com.fabbitinc.server.application.issue.query.result;

import java.util.UUID;

public record IssueUserSummaryResult(
        UUID userId,
        String fullName,
        String email,
        String phone,
        String profileImageUrl
) {
}
