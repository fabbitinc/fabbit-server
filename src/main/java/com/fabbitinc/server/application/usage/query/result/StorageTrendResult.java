package com.fabbitinc.server.application.usage.query.result;

import java.util.List;

public record StorageTrendResult(
        List<StorageTrendItemResult> items
) {
    public record StorageTrendItemResult(
            String date,
            long drawing,
            long attachment,
            long other
    ) {
    }
}
