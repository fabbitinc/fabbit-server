package com.fabbitinc.server.presentation.file.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "응답 DTO")
public record BatchCompleteResponse(
        List<FileCompleteResponse> items,
        List<BatchCompleteFailure> failed
) {
}
