package com.fabbitinc.server.application.synthesis.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record SynthesisUploadItem(
        @NotNull(message = "file_id는 필수입니다")
        UUID fileId,
        Map<String, String> rootContext
) {
}
