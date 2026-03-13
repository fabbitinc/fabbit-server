package com.fabbitinc.server.application.part.dto.response;

import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import java.util.UUID;

@Schema(description = "응답 DTO")
public record PartDetailResponse(
        UUID id,
        UUID revisionId,
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
        PartPreviewResponse preview,
        long childrenCount,
        long parentsCount,
        long suppliersCount,
        long filesCount,
        long projectsCount
) {
}
