package com.fabbitinc.server.application.property.api;

import java.util.ArrayList;
import java.util.List;

public record PropertyDefinitionUsageSummary(
        long partRevisionCount,
        long supplierCount,
        long engineeringBomItemCount,
        long partSupplierCount
) {

    public boolean inUse() {
        return totalCount() > 0;
    }

    public long totalCount() {
        return partRevisionCount + supplierCount + engineeringBomItemCount + partSupplierCount;
    }

    public String describe() {
        List<String> usages = new ArrayList<>();
        if (partRevisionCount > 0) {
            usages.add("부품 리비전 %d건".formatted(partRevisionCount));
        }
        if (supplierCount > 0) {
            usages.add("공급사 %d건".formatted(supplierCount));
        }
        if (engineeringBomItemCount > 0) {
            usages.add("BOM 항목 %d건".formatted(engineeringBomItemCount));
        }
        if (partSupplierCount > 0) {
            usages.add("부품-공급사 연결 %d건".formatted(partSupplierCount));
        }
        return String.join(", ", usages);
    }
}
