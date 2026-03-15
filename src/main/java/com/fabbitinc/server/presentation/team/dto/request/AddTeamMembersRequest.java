package com.fabbitinc.server.presentation.team.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

@Schema(description = "요청 DTO")
public record AddTeamMembersRequest(
        @NotEmpty(message = "user_ids는 1개 이상이어야 합니다") List<UUID> userIds
) {
}
