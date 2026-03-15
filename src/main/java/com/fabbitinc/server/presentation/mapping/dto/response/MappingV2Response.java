package com.fabbitinc.server.presentation.mapping.dto.response;

import com.fabbitinc.server.application.mappingv2.model.MappingV2ResultDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "V2 매핑 응답")
public record MappingV2Response(
        @Schema(description = "V2 매핑 ID")
        UUID id,
        @Schema(description = "원본 파일 ID")
        UUID fileId,
        @Schema(description = "매핑 이름")
        String name,
        @Schema(description = "시트명")
        String sheetName,
        @Schema(description = "원본 헤더 목록")
        List<String> originalHeaders,
        @Schema(description = "매핑에 사용된 헤더 목록")
        List<String> mappedHeaders,
        @Schema(description = "저장된 V2 매핑")
        MappingV2ResultDto mapping,
        @Schema(description = "활성 여부")
        boolean active,
        @Schema(description = "사용 횟수")
        int usageCount,
        @Schema(description = "리비전 버전")
        int version,
        @Schema(description = "생성 시각")
        Instant createdAt
) {
}
