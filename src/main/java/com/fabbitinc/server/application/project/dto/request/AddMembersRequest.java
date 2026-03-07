package com.fabbitinc.server.application.project.dto.request;

import com.fabbitinc.server.domain.project.model.ProjectRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

@Schema(description = "프로젝트 멤버 추가 요청")
public record AddMembersRequest(
        @Schema(description = "추가할 사용자 ID 목록")
        @NotEmpty(message = "user_ids는 1개 이상이어야 합니다") List<UUID> userIds,
        @Schema(description = "부여할 프로젝트 역할", example = "EDITOR")
        ProjectRole role
) {
}
