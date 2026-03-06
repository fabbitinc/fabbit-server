package com.fabbitinc.server.application.supplier.query.condition;

public record SupplierListCondition(
        String search,
        int offset,
        int limit
) {
}
