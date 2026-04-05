package com.fabbitinc.server.application.part.query.result;

import java.util.List;
import java.util.UUID;

public record PartCategoryListResult(List<Item> items) {

    public record Item(
            UUID id,
            String name,
            String formatPrefix,
            String formatSuffix,
            int digits,
            boolean autoNumberingEnabled,
            String previewPartNumber
    ) {
    }
}
