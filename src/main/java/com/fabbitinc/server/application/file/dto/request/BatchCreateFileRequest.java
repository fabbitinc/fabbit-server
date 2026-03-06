package com.fabbitinc.server.application.file.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "요청 DTO")
public record BatchCreateFileRequest(
        @NotEmpty(message = "items는 1개 이상이어야 합니다")
        @Size(max = 100, message = "items는 최대 100개까지 가능합니다")
        List<@Valid CreateFileRequest> items
) {
}
