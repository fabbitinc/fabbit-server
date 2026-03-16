package com.fabbitinc.server.application.part.query.result;

import com.fabbitinc.server.application.part.model.PartRevisionDiffChangeType;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PartRevisionDiffResult(
        Revision baseRevision,
        Revision targetRevision,
        PartRevisionDiffSummaryResult summary,
        List<AttributeChange> attributes,
        List<FileChange> files,
        List<BomChange> bom
) {

    public record Revision(
            UUID revisionId,
            String revisionCode,
            PartRevisionStatus status,
            Instant createdAt,
            PartUserSummaryResult createdBy
    ) {
    }

    public record AttributeChange(
            String fieldKey,
            String fieldLabel,
            PartRevisionDiffChangeType changeType,
            String beforeValue,
            String afterValue
    ) {
    }

    public record FileChange(
            String itemType,
            String displayName,
            PartRevisionDiffChangeType changeType
    ) {
    }

    public record BomChange(
            String lineNumber,
            String beforePartNumber,
            String beforeName,
            BigDecimal beforeQuantity,
            String afterPartNumber,
            String afterName,
            BigDecimal afterQuantity,
            PartRevisionDiffChangeType changeType
    ) {
    }
}
