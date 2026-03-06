package com.fabbitinc.server.application.part.usecase.result;

import java.util.List;
import java.util.UUID;

public record AttachPartFilesResult(
        List<UUID> fileIds
) {
}
