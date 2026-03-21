package com.fabbitinc.server.presentation.chat.dto.response;

import com.fabbitinc.server.domain.chat.model.ChatActionRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "챗 액션 확인 응답")
public record ConfirmChatActionResponse(
        @Schema(description = "액션 요청 ID")
        UUID actionRequestId,
        @Schema(description = "액션 요청 상태")
        ChatActionRequestStatus status,
        @Schema(description = "생성된 이슈 ID", nullable = true)
        UUID issueId
) {
}
