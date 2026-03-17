package com.fabbitinc.server.application.workitem.query.result;

import tools.jackson.databind.JsonNode;

public record TimelineRefResult(
        String id,
        String type,
        String label,
        JsonNode meta
) {
}
