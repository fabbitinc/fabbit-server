package com.fabbitinc.server.presentation.file.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

@Schema(description = "요청 DTO")
public record BatchCompleteRequest(
        @NotEmpty(message = "file_ids는 1개 이상이어야 합니다") @Size(max = 100, message = "file_ids는 최대 100개까지 가능합니다") List<UUID> fileIds
) {
}
