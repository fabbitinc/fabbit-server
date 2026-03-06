package com.fabbitinc.server.application.file.service.output;

import java.util.List;

public record BatchCompleteFilesOutput(
        List<FileCompleteOutput> items,
        List<BatchCompleteFailureOutput> failed
) {
}
