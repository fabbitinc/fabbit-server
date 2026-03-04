package com.fabbitinc.server.application.usage.dto.response;

import java.util.List;

public record StorageUsageResponse(
        long bytesUsed,
        long bytesLimit,
        long bytesOverage,
        boolean allowOverage,
        List<StorageCategoryItem> categories
) {
}
