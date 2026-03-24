package com.fabbitinc.server.presentation.engineeringchange.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.List;

@Schema(description = "변경관리 영향 항목 동기화 요청")
public record SyncAffectedItemsRequest(
        @Schema(description = "최종 영향 항목 목록")
        @Valid
        List<EngineeringChangeAffectedItemTargetRequest> items
) {
    public SyncAffectedItemsRequest {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
