package com.fabbitinc.server.presentation.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "챗 메시지 전송 요청")
public record SendChatMessageRequest(
        @Schema(description = "사용자 입력 텍스트", example = "A-1000 품번 찾아줘")
        @NotBlank
        @Size(max = 4000)
        String text
) {
}
