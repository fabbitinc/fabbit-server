package com.fabbitinc.server.application.project.dto.request;

import com.fabbitinc.server.domain.project.model.ProjectRole;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record AddMembersRequest(
        @NotEmpty(message = "user_ids는 1개 이상이어야 합니다")
        List<UUID> userIds,
        ProjectRole role
) {
}
