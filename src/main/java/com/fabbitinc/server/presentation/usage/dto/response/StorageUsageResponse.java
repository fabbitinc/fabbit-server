package com.fabbitinc.server.presentation.usage.dto.response;

import com.fabbitinc.server.application.usage.model.StorageCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "스토리지 사용량 응답")
public record StorageUsageResponse(
        @Schema(description = "사용 중인 총 바이트", example = "1048576")
        long bytesUsed,
        @Schema(description = "플랜 기본 한도 바이트", example = "1073741824")
        long bytesLimit,
        @Schema(description = "초과 사용 바이트", example = "0")
        long bytesOverage,
        @Schema(description = "초과 사용 허용 여부", example = "false")
        boolean allowOverage,
        @Schema(description = "카테고리별 사용량 목록")
        List<StorageCategoryItemResponse> categories
) {
    @Schema(description = "스토리지 카테고리별 사용량 항목")
    public record StorageCategoryItemResponse(
            @Schema(description = "스토리지 카테고리", example = "DRAWING")
            StorageCategory category,
            @Schema(description = "사용 바이트", example = "524288")
            long bytesUsed,
            @Schema(description = "파일 개수", example = "12")
            int fileCount
    ) {
    }
}
