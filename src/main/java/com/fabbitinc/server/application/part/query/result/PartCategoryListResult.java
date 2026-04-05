package com.fabbitinc.server.application.part.query.result;

import com.fabbitinc.server.domain.part.model.PartItemType;
import java.util.List;
import java.util.UUID;

public record PartCategoryListResult(List<Item> items) {

    public record Item(
            UUID id,
            String name,
            PartItemType itemType,
            String prefix,
            String delimiter,
            int digits,
            String previewPartNumber
    ) {
    }
}
