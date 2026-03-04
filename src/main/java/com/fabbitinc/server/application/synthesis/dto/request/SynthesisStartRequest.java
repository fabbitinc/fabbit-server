package com.fabbitinc.server.application.synthesis.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record SynthesisStartRequest(
        @NotNull(message = "mapping_id는 필수입니다")
        UUID mappingId,
        UUID projectId,
        boolean overwrite,
        @NotEmpty(message = "uploads는 최소 1개 이상이어야 합니다")
        @Size(max = 100, message = "uploads는 최대 100개까지 가능합니다")
        List<@Valid SynthesisUploadItem> uploads
) {
}
