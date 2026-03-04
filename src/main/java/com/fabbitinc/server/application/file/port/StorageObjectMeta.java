package com.fabbitinc.server.application.file.port;

public record StorageObjectMeta(
        long contentLength,
        String contentType
) {
}
