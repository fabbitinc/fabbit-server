package com.fabbitinc.server.application.activation.usecase.result;

import java.util.List;

public record QueryGraphResult(
        List<QueryGraphItemResult> results,
        String answer
) {
}
