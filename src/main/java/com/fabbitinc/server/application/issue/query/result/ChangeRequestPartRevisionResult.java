package com.fabbitinc.server.application.issue.query.result;

import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import java.util.UUID;

public record ChangeRequestPartRevisionResult(
        UUID revisionId,
        UUID partId,
        String partNumber,
        String baseRevisionCode,
        String draftKey,
        String name,
        PartRevisionStatus status
) {
}
