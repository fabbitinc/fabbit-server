package com.fabbitinc.server.application.activity.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fabbitinc.server.application.activity.model.ActivityAction;
import com.fabbitinc.server.application.activity.model.ActivityScope;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "응답 DTO")
public record ActivityResponse(
        UUID id,
        ActivityAction action,
        ActivityScope scope,
        UUID actorId,
        String detail,
        Instant createdAt
) {
}
