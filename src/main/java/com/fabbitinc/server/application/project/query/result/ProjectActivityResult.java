package com.fabbitinc.server.application.project.query.result;

import com.fabbitinc.server.application.activity.dto.response.ActivityAction;
import com.fabbitinc.server.application.activity.dto.response.ActivityScope;

import java.time.Instant;
import java.util.UUID;

public record ProjectActivityResult(
        UUID id,
        ActivityAction action,
        ActivityScope scope,
        UUID actorId,
        String detail,
        Instant createdAt
) {
}
