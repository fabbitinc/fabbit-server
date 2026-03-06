package com.fabbitinc.server.application.part.usecase.command;

public record RenameCategoryCommand(
        String oldName,
        String newName
) {
}
