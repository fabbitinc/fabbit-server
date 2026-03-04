package com.fabbitinc.server.application.part.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameCategoryRequest(
        @NotBlank(message = "new_name은 비어 있을 수 없습니다")
        @Size(max = 200, message = "new_name은 최대 200자여야 합니다")
        String newName
) {
}
