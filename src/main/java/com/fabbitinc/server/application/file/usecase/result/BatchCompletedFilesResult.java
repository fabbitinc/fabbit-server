package com.fabbitinc.server.application.file.usecase.result;

import java.util.List;

public record BatchCompletedFilesResult(
        List<CompletedFileResult> items,
        List<BatchCompleteFailureResult> failed
) {
}
