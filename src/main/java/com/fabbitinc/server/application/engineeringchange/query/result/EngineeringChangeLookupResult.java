package com.fabbitinc.server.application.engineeringchange.query.result;

import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeState;
import java.util.List;
import java.util.UUID;

public record EngineeringChangeLookupResult(
        List<Item> items
) {
    public record Item(
            UUID id,
            int number,
            String title,
            EngineeringChangeState state
    ) {
    }
}
