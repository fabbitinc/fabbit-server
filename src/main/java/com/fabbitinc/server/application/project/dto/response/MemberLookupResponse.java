package com.fabbitinc.server.application.project.dto.response;

import java.util.List;

public record MemberLookupResponse(
        List<ProjectUserSummaryResponse> items
) {
}
