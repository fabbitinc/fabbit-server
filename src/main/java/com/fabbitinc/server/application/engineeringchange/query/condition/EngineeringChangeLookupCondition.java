package com.fabbitinc.server.application.engineeringchange.query.condition;

public record EngineeringChangeLookupCondition(
        String search,
        int limit
) {
}
