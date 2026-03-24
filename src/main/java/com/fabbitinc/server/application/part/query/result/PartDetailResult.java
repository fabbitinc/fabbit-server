package com.fabbitinc.server.application.part.query.result;

import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import java.util.Map;
import java.util.UUID;

public record PartDetailResult(
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
        PartPreviewResult preview,
        long draftCount,
        long childrenCount,
        long parentsCount,
        long suppliersCount,
        long filesCount,
        long projectsCount
) {
}
