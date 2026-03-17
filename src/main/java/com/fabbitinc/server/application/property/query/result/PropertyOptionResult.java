package com.fabbitinc.server.application.property.query.result;

public record PropertyOptionResult(
        String value,
        String label,
        Integer displayOrder,
        Boolean active
) {
}
