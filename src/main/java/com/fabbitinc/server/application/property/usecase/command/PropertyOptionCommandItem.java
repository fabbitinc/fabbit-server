package com.fabbitinc.server.application.property.usecase.command;

public record PropertyOptionCommandItem(
        String value,
        String label,
        Integer displayOrder,
        Boolean active
) {
}
