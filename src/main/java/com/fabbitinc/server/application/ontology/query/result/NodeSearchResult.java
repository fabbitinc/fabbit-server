package com.fabbitinc.server.application.ontology.query.result;

import java.util.List;

public record NodeSearchResult(
        String nodeLabel,
        List<NodeSearchItemResult> items
) {
    public record NodeSearchItemResult(
            String value,
            String label
    ) {
    }
}
