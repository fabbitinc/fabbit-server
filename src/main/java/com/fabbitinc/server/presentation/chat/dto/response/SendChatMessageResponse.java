package com.fabbitinc.server.presentation.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "챗 메시지 전송 응답")
public record SendChatMessageResponse(
        @Schema(description = "사용자 메시지 ID")
        UUID messageId,
        @Schema(description = "생성된 실행 ID")
        UUID runId,
        @Schema(description = "실행 상태", example = "QUEUED")
        String status
) {
}
