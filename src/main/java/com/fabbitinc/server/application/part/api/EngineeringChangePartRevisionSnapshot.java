package com.fabbitinc.server.application.part.api;

import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import java.util.UUID;

public record EngineeringChangePartRevisionSnapshot(
        UUID revisionId,
        UUID partId,
        String partNumber,
        String baseRevisionCode,
        String draftKey,
        String name,
        PartRevisionStatus status
) {
}
