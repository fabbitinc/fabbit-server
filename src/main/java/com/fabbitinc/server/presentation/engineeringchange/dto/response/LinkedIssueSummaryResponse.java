package com.fabbitinc.server.presentation.engineeringchange.dto.response;

import com.fabbitinc.server.domain.issue.model.IssueState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "연결 이슈 요약")
public record LinkedIssueSummaryResponse(
        UUID id,
        int number,
        String title,
        IssueState state
) {
}
