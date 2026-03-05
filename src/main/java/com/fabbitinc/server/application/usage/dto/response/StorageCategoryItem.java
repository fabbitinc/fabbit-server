package com.fabbitinc.server.application.usage.dto.response;

public record StorageCategoryItem(
        StorageCategory category,
        long bytesUsed,
        int fileCount
) {
}
