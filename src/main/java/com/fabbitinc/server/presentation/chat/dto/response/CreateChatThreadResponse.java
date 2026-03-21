package com.fabbitinc.server.presentation.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "챗 스레드 생성 응답")
public record CreateChatThreadResponse(
        @Schema(description = "생성된 스레드 ID")
        UUID threadId
) {
}
