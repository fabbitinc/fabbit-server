package com.fabbitinc.server.application.member.query.result;

import java.util.UUID;

public record MemberLookupItemResult(
        UUID userId,
        String fullName,
        String email,
        String phone,
        String profileImageUrl
) {
}
