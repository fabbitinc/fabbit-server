package com.fabbitinc.server.application.part.query.result;

import java.util.List;
import java.util.UUID;

public record PartProjectsResult(
        long total,
        List<Item> items
) {
    public record Item(
            UUID id,
            String name,
            String description
    ) {
    }
}
