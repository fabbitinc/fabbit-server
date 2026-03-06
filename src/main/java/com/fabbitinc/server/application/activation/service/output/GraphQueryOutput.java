package com.fabbitinc.server.application.activation.service.output;

import java.util.List;

public record GraphQueryOutput(
        List<GraphQueryResultOutput> results,
        String answer
) {
}
