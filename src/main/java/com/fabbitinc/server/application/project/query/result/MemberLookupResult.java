package com.fabbitinc.server.application.project.query.result;
import com.fabbitinc.server.application.workitem.query.result.UserSummaryResult;

import java.util.List;

public record MemberLookupResult(
        List<ProjectUserSummaryResult> items
) {
}
