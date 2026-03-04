package com.fabbitinc.server.application.part.dto.response;

import java.util.List;

public record PartLookupResponse(
        List<PartLookupItemResponse> items
) {
}
