package com.fabbitinc.server.application.file.dto.response;

import java.util.List;

public record BatchCompleteResponse(
        List<FileCompleteResponse> items,
        List<BatchCompleteFailure> failed
) {
}
