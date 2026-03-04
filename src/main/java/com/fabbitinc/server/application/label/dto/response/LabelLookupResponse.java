package com.fabbitinc.server.application.label.dto.response;

import java.util.List;

public record LabelLookupResponse(
        List<LabelLookupItemResponse> items
) {
}
