package com.fabbitinc.server.application.part.dto.response;

import com.fabbitinc.server.domain.part.model.PartLifecycleState;

import java.util.Map;
import java.util.UUID;

public record PartDetailResponse(
        UUID id,
        String partNumber,
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
        UUID ownerId,
        PartOwnerUserSummaryResponse owner,
        UUID ownerTeamId,
        String ownerTeamName,
        RelatedDrawingResponse drawing,
        long childrenCount,
        long parentsCount,
        long suppliersCount,
        long filesCount,
        long projectsCount
) {
}
