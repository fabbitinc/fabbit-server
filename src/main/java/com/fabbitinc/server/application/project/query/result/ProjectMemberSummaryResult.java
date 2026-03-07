package com.fabbitinc.server.application.project.query.result;

import com.fabbitinc.server.domain.project.model.ProjectRole;
import java.util.UUID;

public record ProjectMemberSummaryResult(
        UUID userId,
        String fullName,
        String email,
        String phone,
        String profileImageUrl,
        ProjectRole role
) {
}
