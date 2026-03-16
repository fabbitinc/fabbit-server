package com.fabbitinc.server.application.engineeringchange.api;

import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeState;
import java.util.UUID;

public record EngineeringChangeSnapshot(
        UUID id,
        int number,
        String title,
        EngineeringChangeState state
) {
}
