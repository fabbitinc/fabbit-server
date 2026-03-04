package com.fabbitinc.server.application.part.dto.response;

import java.util.UUID;

public record PartOwnerUserSummaryResponse(
        UUID userId,
        String fullName,
        String email,
        String phone,
        String profileImageUrl
) {
}
