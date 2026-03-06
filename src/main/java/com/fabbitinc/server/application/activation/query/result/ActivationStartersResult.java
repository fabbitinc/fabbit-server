package com.fabbitinc.server.application.activation.query.result;

import java.util.List;

public record ActivationStartersResult(
        List<ActivationStarterQuestionResult> starters
) {
    public ActivationStartersResult {
        starters = starters == null ? List.of() : List.copyOf(starters);
    }
}
