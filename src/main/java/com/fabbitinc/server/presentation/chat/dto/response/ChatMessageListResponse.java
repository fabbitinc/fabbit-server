package com.fabbitinc.server.presentation.chat.dto.response;

import com.fabbitinc.server.domain.chat.model.ChatMessageRole;
import com.fabbitinc.server.domain.chat.model.ChatMessageStatus;
import com.fabbitinc.server.domain.chat.model.ChatMessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

@Schema(description = "챗 메시지 목록 응답")
public record ChatMessageListResponse(
        @Schema(description = "메시지 목록")
        List<ChatMessageResponse> items
) {
    @Schema(description = "챗 메시지")
    public record ChatMessageResponse(
            @Schema(description = "메시지 ID")
            UUID messageId,
            @Schema(description = "실행 ID", nullable = true)
            UUID runId,
            @Schema(description = "메시지 역할")
            ChatMessageRole role,
            @Schema(description = "메시지 타입")
            ChatMessageType messageType,
            @Schema(description = "메시지 상태")
            ChatMessageStatus status,
            @Schema(description = "메시지 순서")
            long sequence,
            @Schema(description = "메시지 내용")
            JsonNode content,
            @Schema(description = "생성 시각")
            Instant createdAt
    ) {
    }
}
