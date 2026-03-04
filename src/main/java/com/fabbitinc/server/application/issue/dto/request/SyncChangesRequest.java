package com.fabbitinc.server.application.issue.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "Issue-CR 동기화 요청")
public record SyncChangesRequest(
        @Schema(description = "최종 변경요청 ID 목록")
        List<UUID> crIds
) {
    public SyncChangesRequest {
        crIds = crIds == null ? List.of() : List.copyOf(crIds);
    }
}
