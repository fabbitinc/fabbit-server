package com.fabbitinc.server.application.project.query.condition;

public record ProjectListCondition(
        String search,
        int offset,
        int limit
) {
}
