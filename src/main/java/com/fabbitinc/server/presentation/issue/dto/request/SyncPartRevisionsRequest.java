package com.fabbitinc.server.presentation.issue.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.List;

@Schema(description = "변경요청 부품 초안 동기화 요청")
public record SyncPartRevisionsRequest(
        @Schema(description = "최종 초안 목록")
        @Valid
        List<ChangeRequestPartRevisionTargetRequest> items
) {
    public SyncPartRevisionsRequest {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
