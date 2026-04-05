package com.fabbitinc.server.application.part.usecase.result;

import java.util.UUID;

public record PartCategoryResult(
        UUID id,
        String name,
        String prefix,
        String delimiter,
        int digits,
        String previewPartNumber
) {
}
