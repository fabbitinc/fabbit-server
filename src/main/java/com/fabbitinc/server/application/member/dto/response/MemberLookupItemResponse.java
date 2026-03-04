package com.fabbitinc.server.application.member.dto.response;

import java.util.UUID;

public record MemberLookupItemResponse(
        UUID userId,
        String fullName,
        String email,
        String phone,
        String profileImageUrl
) {
}
