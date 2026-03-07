package com.fabbitinc.server.application.issue.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "부품 동기화 요청")
public record SyncPartsRequest(
        @Schema(description = "최종 부품 ID 목록")
        List<UUID> partIds
) {
    public SyncPartsRequest {
        partIds = partIds == null ? List.of() : List.copyOf(partIds);
    }
}
