package com.fabbitinc.server.presentation.usage.dto.response;

import java.util.List;

public record StorageTrendResponse(
        List<StorageTrendItemResponse> items
) {
    public record StorageTrendItemResponse(
            String date,
            long drawing,
            long attachment,
            long other
    ) {
    }
}
