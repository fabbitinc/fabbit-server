package com.fabbitinc.server.application.part.query.condition;

import java.util.List;
import java.util.UUID;

public record FileItemsCondition(
        List<UUID> fileIds
) {
}
