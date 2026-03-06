package com.fabbitinc.server.application.part.query.result;

import java.util.UUID;

public record PartUserSummaryResult(
        UUID userId,
        String fullName,
        String email,
        String phone,
        String profileImageUrl
) {
}
