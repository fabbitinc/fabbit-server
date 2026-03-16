package com.fabbitinc.server.application.engineeringchange.query.condition;

public record EngineeringChangeListCondition(
        String search,
        String state,
        int offset,
        int limit
) {
}
