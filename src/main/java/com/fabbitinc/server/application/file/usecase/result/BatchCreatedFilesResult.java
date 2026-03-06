package com.fabbitinc.server.application.file.usecase.result;

import java.util.List;

public record BatchCreatedFilesResult(
        List<CreatedFileResult> items
) {
}
