package com.fabbitinc.server.application.engineeringchange.query.condition;

import java.util.UUID;

public record ChangeFeedCondition(UUID partId, int offset, int limit) {
}
