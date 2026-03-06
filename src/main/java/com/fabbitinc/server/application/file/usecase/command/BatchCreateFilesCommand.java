package com.fabbitinc.server.application.file.usecase.command;

import java.util.List;

public record BatchCreateFilesCommand(
        List<CreateFileCommand> items
) {
}
