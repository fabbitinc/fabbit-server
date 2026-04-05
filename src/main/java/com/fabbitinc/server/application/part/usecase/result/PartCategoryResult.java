package com.fabbitinc.server.application.part.usecase.result;

import java.util.UUID;

public record PartCategoryResult(
        UUID id,
        String name,
        String formatPrefix,
        String formatSuffix,
        int digits,
        boolean autoNumberingEnabled,
        String previewPartNumber
) {
}
