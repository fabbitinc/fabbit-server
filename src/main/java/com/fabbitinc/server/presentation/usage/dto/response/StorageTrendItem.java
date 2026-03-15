package com.fabbitinc.server.presentation.usage.dto.response;

public record StorageTrendItem(
        String date,
        long drawing,
        long attachment,
        long other
) {
}
