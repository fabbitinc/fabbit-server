package com.fabbitinc.server.application.synthesis.dto.response;

import java.util.List;

public record SynthesisListResponse(
        List<SynthesisJobResponse> items
) {
}
