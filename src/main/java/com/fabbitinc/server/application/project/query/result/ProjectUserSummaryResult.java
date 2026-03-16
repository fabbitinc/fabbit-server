package com.fabbitinc.server.application.project.query.result;
import com.fabbitinc.server.application.workitem.query.result.UserSummaryResult;

import java.util.UUID;

public record ProjectUserSummaryResult(
        UUID id,
        String fullName,
        String email,
        String phone,
        String profileImageUrl
) {
}
