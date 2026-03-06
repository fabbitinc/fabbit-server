package com.fabbitinc.server.application.label.usecase.command;

import java.util.UUID;

public record DeleteLabelCommand(
        UUID labelId
) {
}
