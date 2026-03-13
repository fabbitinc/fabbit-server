package com.fabbitinc.server.application.part.query.condition;

import java.util.UUID;

public record PartDraftDetailCondition(
        String partNumber,
        UUID draftId
) {
}
