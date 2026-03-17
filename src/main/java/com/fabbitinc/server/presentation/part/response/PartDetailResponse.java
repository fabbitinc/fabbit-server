package com.fabbitinc.server.presentation.part.response;

import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import java.util.UUID;

@Schema(description = "응답 DTO")
public record PartDetailResponse(
        UUID id,
        UUID revisionId,
        PartRevisionStatus revisionStatus,
        String partNumber,
        UUID baseRevisionId,
        String baseRevisionCode,
        String name,
        String revision,
        String material,
        String unit,
        String description,
        String category,
        PartLifecycleState lifecycleState,
        Boolean isPhantom,
        Integer leadTimeDays,
        Map<String, Object> extendedProperties,
        PartPreviewResponse preview,
        long draftCount,
        long childrenCount,
        long parentsCount,
        long suppliersCount,
        long filesCount,
        long projectsCount
) {
}
