package com.fabbitinc.server.presentation.engineeringchange.dto.response;

import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeState;
import com.fabbitinc.server.presentation.file.dto.response.FileItemResponse;
import com.fabbitinc.server.presentation.workitem.dto.response.UserSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "변경관리 목록 항목")
public record EngineeringChangeSummaryResponse(
        UUID id,
        int number,
        String title,
        EngineeringChangeState state,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt,
        UserSummaryResponse createdBy,
        List<LabelBadgeResponse> labels,
        List<EngineeringChangeStepResponse> steps,
        List<FileItemResponse> files,
        int commentsCount,
        Instant releasedAt,
        UserSummaryResponse releasedBy
) {
}
