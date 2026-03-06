package com.fabbitinc.server.application.mapping.usecase.command;

import java.util.UUID;

public record DeactivateMappingCommand(
        UUID mappingId
) {
}
