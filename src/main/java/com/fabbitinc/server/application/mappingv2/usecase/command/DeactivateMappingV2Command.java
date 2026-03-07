package com.fabbitinc.server.application.mappingv2.usecase.command;

import java.util.UUID;

public record DeactivateMappingV2Command(
        UUID mappingId
) {
}
