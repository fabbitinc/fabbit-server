package com.fabbitinc.server.application.project.query.result;

import java.util.List;

public record ProjectMemberListResult(
        List<ProjectMemberSummaryResult> items
) {
}
