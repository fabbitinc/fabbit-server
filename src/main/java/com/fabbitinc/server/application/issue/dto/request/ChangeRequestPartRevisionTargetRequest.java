package com.fabbitinc.server.application.issue.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "변경요청에 연결할 부품 초안 식별자")
public record ChangeRequestPartRevisionTargetRequest(
        @Schema(description = "품번", example = "AES-100")
        @NotBlank(message = "partNumber는 필수입니다")
        String partNumber,
        @Schema(description = "기준 공식 리비전 코드. 초기 초안이면 null", example = "1")
        String baseRevisionCode,
        @Schema(description = "초안 키", example = "D1")
        @NotBlank(message = "draftKey는 필수입니다")
        String draftKey
) {
}
