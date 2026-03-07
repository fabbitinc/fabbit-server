package com.fabbitinc.server.application.file.port;

import java.util.List;

public record StorageDeleteResult(
        List<String> deletedKeys,
        List<String> failedKeys
) {
    public int deletedCount() {
        return deletedKeys.size();
    }
}
