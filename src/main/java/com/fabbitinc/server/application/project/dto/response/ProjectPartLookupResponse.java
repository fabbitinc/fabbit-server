package com.fabbitinc.server.application.project.dto.response;

import java.util.List;

public record ProjectPartLookupResponse(
        List<ProjectPartLookupItemResponse> items
) {
}
