package com.fabbitinc.server.presentation.project.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

@Schema(description = "프로젝트 멤버 제거 요청")
public record ManageMembersRequest(
        @Schema(description = "제거할 사용자 ID 목록")
        @NotEmpty(message = "user_ids는 1개 이상이어야 합니다") List<UUID> userIds
) {
}
