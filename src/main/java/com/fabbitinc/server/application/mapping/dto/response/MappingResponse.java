package com.fabbitinc.server.application.mapping.dto.response;

import com.fabbitinc.server.application.mapping.dto.common.MappingResultDto;
import com.fabbitinc.server.domain.mapping.model.MappingScope;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "매핑 상세 응답")
public record MappingResponse(
        @Schema(description = "매핑 ID")
        UUID id,
        @Schema(description = "원본 파일 ID")
        UUID fileId,
        @Schema(description = "매핑 이름")
        String name,
        @Schema(description = "시트명")
        String sheetName,
        @Schema(description = "원본 헤더 목록")
        List<String> originalHeaders,
        @Schema(description = "실사용 컬럼 목록")
        List<String> mappedHeaders,
        @Schema(description = "매핑 본문")
        MappingResultDto mapping,
        @Schema(description = "매핑 스코프", example = "FULL_BOM")
        MappingScope scope,
        @Schema(description = "활성 여부")
        boolean isActive,
        @Schema(description = "누적 사용 횟수")
        int usageCount,
        @Schema(description = "리비전 버전")
        int version,
        @Schema(description = "생성 시각")
        Instant createdAt
) {
}
