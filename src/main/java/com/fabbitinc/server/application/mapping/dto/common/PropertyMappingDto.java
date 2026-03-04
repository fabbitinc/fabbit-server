package com.fabbitinc.server.application.mapping.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Part 속성 매핑")
public record PropertyMappingDto(
        @Schema(description = "원본 컬럼명", example = "품번")
        String sourceColumn,
        @Schema(description = "대상 속성명", example = "part_number")
        String targetProperty,
        @Schema(description = "데이터 타입", example = "string")
        String dataType,
        @Schema(description = "매핑 신뢰도(0-100)", example = "95")
        Integer confidence,
        @Schema(description = "매핑 근거", example = "header exact match")
        String reason,
        @Schema(description = "확장 속성 여부", example = "false")
        Boolean isExtended
) {
    public PropertyMappingDto {
        dataType = normalizeDataType(dataType);
        confidence = confidence == null ? 0 : confidence;
        reason = reason == null ? "" : reason;
        isExtended = isExtended != null && isExtended;
    }

    private static String normalizeDataType(String raw) {
        if (raw == null || raw.isBlank()) {
            return "string";
        }
        String normalized = raw.trim().toLowerCase();
        return switch (normalized) {
            case "string", "integer", "float", "boolean" -> normalized;
            default -> "string";
        };
    }
}
