package com.fabbitinc.server.application.workitem.query.result;

import java.util.UUID;

public record UserSummaryResult(
        UUID userId,
        String fullName,
        String email,
        String phone,
        String profileImageUrl
) {
}
