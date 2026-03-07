package com.fabbitinc.server.application.part.query.result;

import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import java.util.List;

public record PartFilterOptionsResult(
        List<String> categories,
        List<PartLifecycleState> lifecycleStates
) {
}
