package com.fabbitinc.server.application.part.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fabbitinc.server.domain.part.model.PartLifecycleState;

import java.util.List;

@Schema(description = "응답 DTO")
public record PartFilterOptionsResponse(
        List<String> categories,
        List<PartLifecycleState> lifecycleStates
) {
}
