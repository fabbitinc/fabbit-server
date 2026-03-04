package com.fabbitinc.server.application.part.dto.response;

import java.util.List;

public record CategoryLookupResponse(
        List<String> items
) {
}
