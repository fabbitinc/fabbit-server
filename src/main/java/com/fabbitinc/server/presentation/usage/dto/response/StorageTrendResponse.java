package com.fabbitinc.server.presentation.usage.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "스토리지 사용량 추이 응답")
public record StorageTrendResponse(
        @Schema(description = "일자별 추이 목록")
        List<StorageTrendItemResponse> items
) {
    @Schema(description = "스토리지 추이 항목")
    public record StorageTrendItemResponse(
            @Schema(description = "집계 일자", example = "2026-03-07")
            String date,
            @Schema(description = "도면 스토리지 사용량", example = "1024")
            long drawing,
            @Schema(description = "첨부 파일 스토리지 사용량", example = "2048")
            long attachment,
            @Schema(description = "기타 스토리지 사용량", example = "512")
            long other
    ) {
    }
}
