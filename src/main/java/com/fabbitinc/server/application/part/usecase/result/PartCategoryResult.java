package com.fabbitinc.server.application.part.usecase.result;

import com.fabbitinc.server.domain.part.model.PartItemType;
import java.util.UUID;

public record PartCategoryResult(
        UUID id,
        String name,
        PartItemType itemType,
        String prefix,
        String delimiter,
        int digits,
        String previewPartNumber
) {
}
