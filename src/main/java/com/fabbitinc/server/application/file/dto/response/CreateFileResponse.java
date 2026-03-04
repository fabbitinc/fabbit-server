package com.fabbitinc.server.application.file.dto.response;

import java.util.UUID;

public record CreateFileResponse(
        UUID fileId,
        String uploadUrl,
        String fileKey
) {
}
