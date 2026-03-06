package com.fabbitinc.server.application.file.service.output;

import java.util.UUID;

public record CreateFileOutput(
        UUID fileId,
        String uploadUrl,
        String fileKey
) {
}
