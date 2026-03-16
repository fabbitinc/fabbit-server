package com.fabbitinc.server.presentation.issue.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "연결 변경관리 동기화 요청")
public record SyncLinkedEngineeringChangesRequest(
        @Schema(description = "최종 변경관리 ID 목록")
        List<UUID> engineeringChangeIds
) {
    public SyncLinkedEngineeringChangesRequest {
        engineeringChangeIds = engineeringChangeIds == null ? List.of() : List.copyOf(engineeringChangeIds);
    }
}
