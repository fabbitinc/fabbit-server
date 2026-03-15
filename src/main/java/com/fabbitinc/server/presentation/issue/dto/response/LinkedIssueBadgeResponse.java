package com.fabbitinc.server.presentation.issue.dto.response;

import com.fabbitinc.server.domain.issue.model.IssueState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "연결 이슈 배지")
public record LinkedIssueBadgeResponse(
        UUID id,
        int number,
        String title,
        IssueState state
) {
}
