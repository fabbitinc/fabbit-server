package com.fabbitinc.server.application.usage.dto.response;

public record StorageTrendItem(
        String date,
        long drawing,
        long attachment,
        long other
) {
}
