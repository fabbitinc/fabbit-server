package com.fabbitinc.server.application.project.query.result;

import java.util.List;

public record MemberLookupResult(
        List<ProjectUserSummaryResult> items
) {
}
