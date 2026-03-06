package com.fabbitinc.server.application.part.query.result;

import java.util.List;

public record CategoryStatsResult(
        List<Item> items
) {
    public record Item(
            String category,
            long partCount
    ) {
    }
}
