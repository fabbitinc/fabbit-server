package com.fabbitinc.server.application.part.dto.response;

import java.util.List;

public record PartFilterOptionsResponse(
        List<String> categories,
        List<String> lifecycleStates
) {
}
