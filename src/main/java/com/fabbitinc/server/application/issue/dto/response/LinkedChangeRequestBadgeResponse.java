package com.fabbitinc.server.application.issue.dto.response;

import com.fabbitinc.server.domain.issue.model.CrState;
import com.fabbitinc.server.domain.issue.model.IssueState;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "연결 변경요청 배지")
public record LinkedChangeRequestBadgeResponse(
        UUID id,
        int number,
        String title,
        IssueState state,
        CrState crState
) {
}
