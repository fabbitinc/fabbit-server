package com.fabbitinc.server.application.part.dto.response;

import java.util.UUID;

public record RelatedSupplierResponse(
        UUID id,
        String companyName,
        String code,
        String country,
        Double unitCost
) {
}
