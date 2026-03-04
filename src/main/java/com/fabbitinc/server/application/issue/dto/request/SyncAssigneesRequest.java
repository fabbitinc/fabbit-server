package com.fabbitinc.server.application.issue.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "개인 담당자 동기화 요청")
public record SyncAssigneesRequest(
        @Schema(description = "최종 담당자 ID 목록")
        List<UUID> userIds
) {
    public SyncAssigneesRequest {
        userIds = userIds == null ? List.of() : List.copyOf(userIds);
    }
}
