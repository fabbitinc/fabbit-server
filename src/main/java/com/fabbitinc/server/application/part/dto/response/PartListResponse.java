package com.fabbitinc.server.application.part.dto.response;

import java.util.List;

public record PartListResponse(
        long total,
        int offset,
        int limit,
        List<PartSummaryResponse> items
) {
}
