package com.fabbitinc.server.application.usage.query.result;

import com.fabbitinc.server.application.usage.model.StorageCategory;
import java.util.List;

public record StorageUsageResult(
        long bytesUsed,
        long bytesLimit,
        long bytesOverage,
        boolean allowOverage,
        List<StorageCategoryItemResult> categories
) {
    public record StorageCategoryItemResult(
            StorageCategory category,
            long bytesUsed,
            int fileCount
    ) {
    }
}
