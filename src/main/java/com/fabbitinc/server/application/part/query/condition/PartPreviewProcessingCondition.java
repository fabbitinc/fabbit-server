package com.fabbitinc.server.application.part.query.condition;

import java.util.UUID;

public record PartPreviewProcessingCondition(
        UUID partId,
        UUID revisionId
) {
}
