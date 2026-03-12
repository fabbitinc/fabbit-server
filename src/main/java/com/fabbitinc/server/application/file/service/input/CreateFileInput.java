package com.fabbitinc.server.application.file.service.input;

public record CreateFileInput(
        String originalName,
        String contentType,
        long fileSize,
        String contentHash
) {
}
