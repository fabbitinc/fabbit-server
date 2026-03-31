package com.fabbitinc.server.presentation.migration.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "Inventor 마이그레이션 커밋 요청")
public record CommitInventorMigrationRequest(
        @Schema(description = "마이그레이션 세션 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull(message = "sessionId는 필수입니다")
        UUID sessionId
) {
}
