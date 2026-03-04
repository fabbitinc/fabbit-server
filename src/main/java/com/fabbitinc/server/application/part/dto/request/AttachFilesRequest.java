package com.fabbitinc.server.application.part.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record AttachFilesRequest(
        @NotEmpty(message = "file_ids는 1개 이상이어야 합니다")
        @Size(max = 20, message = "file_ids는 최대 20개까지 가능합니다")
        List<UUID> fileIds
) {
}
