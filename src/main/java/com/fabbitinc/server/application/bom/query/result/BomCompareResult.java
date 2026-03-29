package com.fabbitinc.server.application.bom.query.result;

import com.fabbitinc.server.application.part.model.PartRevisionDiffChangeType;
import java.math.BigDecimal;
import java.util.List;

public record BomCompareResult(
        List<Change> changes,
        Summary summary
) {

    public record Change(
            String lineNumber,
            PartRevisionDiffChangeType changeType,
            String sourcePartNumber,
            String sourceName,
            BigDecimal sourceQuantity,
            String targetPartNumber,
            String targetName,
            BigDecimal targetQuantity
    ) {
    }

    public record Summary(
            int addedCount,
            int removedCount,
            int changedCount,
            int unchangedCount,
            int totalCount
    ) {
    }
}
