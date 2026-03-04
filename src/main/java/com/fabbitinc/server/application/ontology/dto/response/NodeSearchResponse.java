package com.fabbitinc.server.application.ontology.dto.response;

import java.util.List;

public record NodeSearchResponse(
        String nodeLabel,
        List<NodeSearchItemResponse> items
) {
}
