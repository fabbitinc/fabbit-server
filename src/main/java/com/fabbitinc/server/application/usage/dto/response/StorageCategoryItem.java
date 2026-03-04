package com.fabbitinc.server.application.usage.dto.response;

public record StorageCategoryItem(
        String category,
        long bytesUsed,
        int fileCount
) {
}
