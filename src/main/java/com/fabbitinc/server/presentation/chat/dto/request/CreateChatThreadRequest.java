package com.fabbitinc.server.presentation.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "챗 스레드 생성 요청")
public record CreateChatThreadRequest(
        @Schema(description = "프로젝트 문맥 ID", nullable = true)
        UUID projectId,
        @Schema(description = "문맥 타입", example = "GLOBAL")
        String contextType,
        @Schema(description = "문맥 대상 ID", nullable = true)
        UUID contextId,
        @Schema(description = "스레드 제목", example = "부품 질의")
        String title
) {
}
