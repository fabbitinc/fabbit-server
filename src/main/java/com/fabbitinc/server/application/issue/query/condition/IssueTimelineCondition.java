package com.fabbitinc.server.application.issue.query.condition;

import com.fabbitinc.server.application.issue.support.IssueTargetType;

public record IssueTimelineCondition(
        int issueNumber,
        IssueTargetType targetType
) {
}
