package com.fabbitinc.server.presentation.usage.dto.response;

import com.fabbitinc.server.application.usage.model.StorageCategory;

public record StorageCategoryItem(
        StorageCategory category,
        long bytesUsed,
        int fileCount
) {
}
