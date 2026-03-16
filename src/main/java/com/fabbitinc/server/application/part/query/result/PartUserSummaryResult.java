package com.fabbitinc.server.application.part.query.result;
import com.fabbitinc.server.application.workitem.query.result.UserSummaryResult;

import java.util.UUID;

public record PartUserSummaryResult(
        UUID userId,
        String fullName,
        String email,
        String phone,
        String profileImageUrl
) {
}
