package com.fabbitinc.server.presentation.engineeringchange.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "변경관리 라벨 동기화 요청")
public record SyncLabelsRequest(
        @Schema(description = "최종 라벨 ID 목록")
        List<UUID> labelIds
) {
    public SyncLabelsRequest {
        labelIds = labelIds == null ? List.of() : List.copyOf(labelIds);
    }
}
