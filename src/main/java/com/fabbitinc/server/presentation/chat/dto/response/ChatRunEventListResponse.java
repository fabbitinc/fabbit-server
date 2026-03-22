package com.fabbitinc.server.presentation.chat.dto.response;

import com.fabbitinc.server.domain.chat.model.ChatRunEventVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

@Schema(description = "챗 실행 이벤트 목록 응답")
public record ChatRunEventListResponse(
        @Schema(description = "실행 이벤트 목록")
        List<ChatRunEventResponse> items
) {
    @Schema(description = "챗 실행 이벤트")
    public record ChatRunEventResponse(
            @Schema(description = "이벤트 ID")
            UUID eventId,
            @Schema(description = "실행 ID")
            UUID runId,
            @Schema(description = "이벤트 순서")
            long sequence,
            @Schema(description = "이벤트 타입")
            String eventType,
            @Schema(description = "이벤트 노출 수준")
            ChatRunEventVisibility visibility,
            @Schema(description = "이벤트 payload")
            JsonNode payload,
            @Schema(description = "생성 시각")
            Instant createdAt
    ) {
    }
}
