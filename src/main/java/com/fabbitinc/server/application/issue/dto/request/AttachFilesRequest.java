package com.fabbitinc.server.application.issue.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(description = "첨부파일 연결 요청")
public record AttachFilesRequest(
        @NotEmpty
        @Size(max = 20)
        @Schema(description = "연결할 파일 ID 목록")
        List<UUID> fileIds
) {
}
