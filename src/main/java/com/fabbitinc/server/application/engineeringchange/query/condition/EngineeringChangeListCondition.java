package com.fabbitinc.server.application.engineeringchange.query.condition;

public record EngineeringChangeListCondition(
        String search,
        EngineeringChangeStateFilter state,
        int offset,
        int limit
) {
}
