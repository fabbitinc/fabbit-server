package com.fabbitinc.server.application.project.query.condition;

import java.util.UUID;

public record ProjectMembersCondition(
        UUID projectId
) {
}
