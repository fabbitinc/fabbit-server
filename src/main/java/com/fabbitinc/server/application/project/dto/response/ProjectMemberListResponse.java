package com.fabbitinc.server.application.project.dto.response;

import java.util.List;

public record ProjectMemberListResponse(
        List<ProjectMemberSummaryResponse> items
) {
}
