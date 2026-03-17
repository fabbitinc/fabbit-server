package com.fabbitinc.server.presentation.workitem.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "이슈 타임라인 응답")
public record TimelineResponse(
        List<TimelineItemResponse> items
) {
}
