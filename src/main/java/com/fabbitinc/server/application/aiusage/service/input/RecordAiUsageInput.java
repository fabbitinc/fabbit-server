package com.fabbitinc.server.application.aiusage.service.input;

import com.fabbitinc.server.domain.aiusage.model.AiUsageCategory;

import java.util.UUID;

public record RecordAiUsageInput(
        UUID orgId,
        UUID userId,
        AiUsageCategory category,
        String feature,
        String model,
        int inputTokens,
        int outputTokens
) {
}
