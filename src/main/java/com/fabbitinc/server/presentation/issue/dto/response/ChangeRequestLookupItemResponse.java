package com.fabbitinc.server.presentation.issue.dto.response;

import com.fabbitinc.server.domain.issue.model.CrState;
import com.fabbitinc.server.domain.issue.model.IssueState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "변경요청 lookup 항목")
public record ChangeRequestLookupItemResponse(
        UUID id,
        int number,
        String title,
        IssueState state,
        CrState crState
) {
}
