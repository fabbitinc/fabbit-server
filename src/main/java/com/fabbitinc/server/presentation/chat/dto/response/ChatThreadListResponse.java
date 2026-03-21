package com.fabbitinc.server.presentation.chat.dto.response;

import com.fabbitinc.server.domain.chat.model.ChatThreadStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "챗 스레드 목록 응답")
public record ChatThreadListResponse(
        @Schema(description = "스레드 목록")
        List<ChatThreadItemResponse> items
) {
    @Schema(description = "챗 스레드 요약")
    public record ChatThreadItemResponse(
            @Schema(description = "스레드 ID")
            UUID threadId,
            @Schema(description = "프로젝트 ID", nullable = true)
            UUID projectId,
            @Schema(description = "문맥 타입", example = "GLOBAL")
            String contextType,
            @Schema(description = "문맥 대상 ID", nullable = true)
            UUID contextId,
            @Schema(description = "스레드 제목")
            String title,
            @Schema(description = "스레드 상태")
            ChatThreadStatus status,
            @Schema(description = "마지막 메시지 시각")
            Instant lastMessageAt,
            @Schema(description = "생성 시각")
            Instant createdAt
    ) {
    }
}
