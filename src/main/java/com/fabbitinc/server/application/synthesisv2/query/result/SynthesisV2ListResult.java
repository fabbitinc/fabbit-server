package com.fabbitinc.server.application.synthesisv2.query.result;

import java.util.List;

public record SynthesisV2ListResult(
        List<SynthesisV2JobResult> items
) {
}
