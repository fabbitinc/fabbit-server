package com.fabbitinc.server.application.workitem.usecase.result;

import java.util.UUID;

public record CommentUserSummaryResult(
        UUID userId,
        String fullName,
        String email,
        String phone,
        String profileImageUrl
) {
}
