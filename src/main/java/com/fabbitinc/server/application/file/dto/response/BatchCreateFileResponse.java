package com.fabbitinc.server.application.file.dto.response;

import java.util.List;

public record BatchCreateFileResponse(
        List<CreateFileResponse> items
) {
}
