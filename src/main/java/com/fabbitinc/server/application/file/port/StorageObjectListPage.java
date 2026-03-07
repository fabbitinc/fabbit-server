package com.fabbitinc.server.application.file.port;

import java.util.List;

public record StorageObjectListPage(
        List<String> objectKeys,
        String nextContinuationToken
) {
    public boolean hasNextPage() {
        return nextContinuationToken != null && !nextContinuationToken.isBlank();
    }
}
