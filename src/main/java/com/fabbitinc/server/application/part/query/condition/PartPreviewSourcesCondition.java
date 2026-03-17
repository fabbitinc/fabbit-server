package com.fabbitinc.server.application.part.query.condition;

import java.util.UUID;

public record PartPreviewSourcesCondition(
        UUID partId,
        UUID revisionId
) {
}
