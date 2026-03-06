package com.fabbitinc.server.application.synthesis.query.result;

import java.util.List;

public record SynthesisListResult(
        List<SynthesisJobResult> items
) {
}
