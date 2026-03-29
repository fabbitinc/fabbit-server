package com.fabbitinc.server.application.bom.usecase.result;

import java.math.BigDecimal;
import java.util.List;

public record PreviewBomImportResult(
        List<RowResult> rows,
        Summary summary
) {

    public record RowResult(
            int rowNumber,
            String lineNumber,
            String childPartNumber,
            String childRevisionCode,
            BigDecimal quantity,
            RowStatus status,
            String message
    ) {
    }

    public enum RowStatus {
        SUCCESS,
        ERROR,
        WARNING
    }

    public record Summary(
            int totalCount,
            int successCount,
            int errorCount,
            int warningCount
    ) {
    }
}
