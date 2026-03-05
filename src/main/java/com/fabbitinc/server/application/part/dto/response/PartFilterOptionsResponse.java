package com.fabbitinc.server.application.part.dto.response;

import com.fabbitinc.server.domain.part.model.PartLifecycleState;

import java.util.List;

public record PartFilterOptionsResponse(
        List<String> categories,
        List<PartLifecycleState> lifecycleStates
) {
}
