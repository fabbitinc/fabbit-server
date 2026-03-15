package com.fabbitinc.server.presentation.part.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "요청 DTO")
public record RenameCategoryRequest(
        @NotBlank(message = "new_name은 비어 있을 수 없습니다") @Size(max = 200, message = "new_name은 최대 200자여야 합니다") String newName
) {
}
