package com.fabbitinc.server.domain.aiusage.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum AiUsageCategory {
    CHAT(1),
    BOM_ANALYSIS(5),
    DRAWING_PARSE(10);

    private final int creditCost;

    AiUsageCategory(int creditCost) {
        this.creditCost = creditCost;
    }

    public int creditCost() {
        return creditCost;
    }

    public BigDecimal creditCostDecimal() {
        return BigDecimal.valueOf(creditCost).setScale(4, RoundingMode.UNNECESSARY);
    }
}
