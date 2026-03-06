package com.fabbitinc.server.application.part.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "응답 DTO")
public record RelatedSupplierResponse(
        UUID id,
        String companyName,
        String code,
        String country,
        Double unitCost
) {
}
