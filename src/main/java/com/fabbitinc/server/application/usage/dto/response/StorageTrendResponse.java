package com.fabbitinc.server.application.usage.dto.response;

import java.util.List;

public record StorageTrendResponse(
        List<StorageTrendItem> items
) {
}
