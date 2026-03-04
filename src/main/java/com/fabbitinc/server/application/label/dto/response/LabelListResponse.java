package com.fabbitinc.server.application.label.dto.response;

import java.util.List;

public record LabelListResponse(
        int total,
        List<LabelResponse> items
) {
}
