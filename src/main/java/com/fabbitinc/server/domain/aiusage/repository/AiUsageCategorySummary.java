package com.fabbitinc.server.domain.aiusage.repository;

import java.math.BigDecimal;

public interface AiUsageCategorySummary {

    String getCategory();

    BigDecimal getCreditsUsed();

    long getUsageCount();
}
