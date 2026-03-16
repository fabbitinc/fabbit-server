package com.fabbitinc.server.presentation.issue.dto.response;

import com.fabbitinc.server.domain.issue.model.EngineeringChangeState;
import com.fabbitinc.server.domain.issue.model.IssueState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "연결 변경관리 배지")
public record LinkedEngineeringChangeBadgeResponse(
        UUID id,
        int number,
        String title,
        IssueState state,
        EngineeringChangeState engineeringChangeState
) {
}
