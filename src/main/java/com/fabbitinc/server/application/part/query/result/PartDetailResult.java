package com.fabbitinc.server.application.part.query.result;

import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import java.util.Map;
import java.util.UUID;

public record PartDetailResult(
        UUID id,
        UUID revisionId,
        String partNumber,
        String name,
        String revision,
        String draftKey,
        String material,
        String unit,
        String description,
        String category,
        PartLifecycleState lifecycleState,
        Boolean isPhantom,
        Integer leadTimeDays,
        Map<String, Object> extendedProperties,
        UUID ownerId,
        PartUserSummaryResult owner,
        UUID ownerTeamId,
        String ownerTeamName,
        PartPreviewResult preview,
        long draftCount,
        long inReviewCount,
        long childrenCount,
        long parentsCount,
        long suppliersCount,
        long filesCount,
        long projectsCount
) {
}
