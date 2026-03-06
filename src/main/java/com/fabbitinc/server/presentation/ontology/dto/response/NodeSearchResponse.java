package com.fabbitinc.server.presentation.ontology.dto.response;

import java.util.List;

public record NodeSearchResponse(
        String nodeLabel,
        List<NodeSearchItemResponse> items
) {
    public record NodeSearchItemResponse(
            String value,
            String label
    ) {
    }
}
