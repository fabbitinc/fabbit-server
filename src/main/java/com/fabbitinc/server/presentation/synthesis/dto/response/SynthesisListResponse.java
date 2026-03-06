package com.fabbitinc.server.presentation.synthesis.dto.response;

import java.util.List;

public record SynthesisListResponse(
        List<SynthesisJobResponse> items
) {
}
