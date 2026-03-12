package com.fabbitinc.server.application.file.usecase.command;

public record CreateFileCommand(
        String originalName,
        String contentType,
        long fileSize,
        String contentHash
) {
}
