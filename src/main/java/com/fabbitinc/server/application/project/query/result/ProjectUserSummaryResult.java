package com.fabbitinc.server.application.project.query.result;
import java.util.UUID;

public record ProjectUserSummaryResult(
        UUID id,
        String fullName,
        String email,
        String phone,
        String profileImageUrl
) {
}
