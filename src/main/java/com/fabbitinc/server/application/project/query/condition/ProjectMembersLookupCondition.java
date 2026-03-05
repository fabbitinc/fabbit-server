package com.fabbitinc.server.application.project.query.condition;

import java.util.UUID;

public record ProjectMembersLookupCondition(
        UUID projectId,
        String search,
        int limit
) {
}
