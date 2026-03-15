package com.fabbitinc.server.presentation.issue.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "검토자 동기화 요청")
public record SyncReviewersRequest(
        @Schema(description = "최종 검토자 ID 목록")
        List<UUID> userIds
) {
    public SyncReviewersRequest {
        userIds = userIds == null ? List.of() : List.copyOf(userIds);
    }
}
