package com.fabbitinc.server.application.mapping.usecase.result;

public record SkippedSheetResult(
        String sheetName,
        String reason
) {
}
