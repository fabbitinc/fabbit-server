package com.fabbitinc.server.application.activation.dto.response;

import java.util.List;

public record QueryResponse(
        List<QueryResultResponse> results,
        String answer
) {
}
