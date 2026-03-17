package com.fabbitinc.server.application.workitem.query.result;

import tools.jackson.databind.JsonNode;

public record TimelineValueChangeResult(
        JsonNode oldValue,
        JsonNode newValue
) {
}
