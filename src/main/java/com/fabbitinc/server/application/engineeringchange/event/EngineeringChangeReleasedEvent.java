package com.fabbitinc.server.application.engineeringchange.event;

import java.util.UUID;

public record EngineeringChangeReleasedEvent(
        UUID engineeringChangeId,
        UUID actorId,
        int ecNumber,
        String ecTitle
) {
}
