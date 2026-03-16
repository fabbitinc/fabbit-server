package com.fabbitinc.server.presentation.workitem.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "동기화 결과")
public record SyncDiffResponse(
        int addedCount,
        int removedCount
) {
}
