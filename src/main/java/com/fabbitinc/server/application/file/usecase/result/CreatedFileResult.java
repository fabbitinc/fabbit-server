package com.fabbitinc.server.application.file.usecase.result;

import java.util.UUID;

public record CreatedFileResult(
        UUID fileId,
        String uploadUrl,
        String fileKey
) {
}
