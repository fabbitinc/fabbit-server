package com.fabbitinc.server.presentation.engineeringchange.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "변경 피드 응답")
public record ChangeFeedResponse(
        @Schema(description = "변경 피드 항목 목록") List<ChangeFeedItemResponse> items
) {

    @Schema(description = "변경 피드 항목")
    public record ChangeFeedItemResponse(
            @Schema(description = "EC 식별자", example = "550e8400-e29b-41d4-a716-446655440000") UUID ecId,
            @Schema(description = "EC 번호", example = "1") int ecNumber,
            @Schema(description = "EC 제목", example = "부품 재질 변경") String title,
            @Schema(description = "영향받는 파트 번호 목록") List<String> affectedPartNumbers,
            @Schema(description = "영향받는 파트 수", example = "3") int affectedPartCount,
            @Schema(description = "릴리즈 일시") Instant releasedAt,
            @Schema(description = "릴리즈한 사용자 식별자") UUID releasedById,
            @Schema(description = "릴리즈한 사용자 이름", example = "홍길동") String releasedByName,
            @Schema(description = "원본 이슈 번호 (nullable)", example = "42") Integer sourceIssueNumber
    ) {
    }
}
