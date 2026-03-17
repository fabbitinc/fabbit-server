package com.fabbitinc.server.application.part.query.condition;

import java.util.UUID;

public record PartFilesCondition(
        UUID partId,
        UUID revisionId
) {
}
