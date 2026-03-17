package com.fabbitinc.server.presentation.workitem.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

@Schema(description = "타임라인 상세 정보")
public record TimelineDetailResponse(
        @Schema(description = "필드별 변경값")
        Map<String, TimelineValueChangeResponse> changes,
        @Schema(description = "일반 참조 목록")
        List<TimelineRefResponse> refs,
        @Schema(description = "추가된 참조 목록")
        List<TimelineRefResponse> added,
        @Schema(description = "제거된 참조 목록")
        List<TimelineRefResponse> removed
) {
}
