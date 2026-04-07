package com.fabbitinc.server.application.engineeringchange.query.result;

import java.util.UUID;

public record LabelBadgeResult(
        UUID id,
        String name,
        String color
) {
}
