package com.fabbitinc.server.application.part.query.condition;

import java.util.UUID;

public record BomTreeCondition(
        UUID partId,
        String direction
) {
}
