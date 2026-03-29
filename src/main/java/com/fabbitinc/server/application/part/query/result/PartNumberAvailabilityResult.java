package com.fabbitinc.server.application.part.query.result;

public record PartNumberAvailabilityResult(
        String partNumber,
        boolean available
) {
}
