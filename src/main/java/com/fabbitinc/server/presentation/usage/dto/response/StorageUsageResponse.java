package com.fabbitinc.server.presentation.usage.dto.response;

import com.fabbitinc.server.application.usage.model.StorageCategory;

import java.util.List;

public record StorageUsageResponse(
        long bytesUsed,
        long bytesLimit,
        long bytesOverage,
        boolean allowOverage,
        List<StorageCategoryItemResponse> categories
) {
    public record StorageCategoryItemResponse(
            StorageCategory category,
            long bytesUsed,
            int fileCount
    ) {
    }
}
