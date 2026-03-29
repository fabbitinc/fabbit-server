package com.fabbitinc.server.application.bom.query;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.bom.query.condition.WhereUsedSummaryCondition;
import com.fabbitinc.server.application.bom.query.result.WhereUsedSummaryResult;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.bom.model.EngineeringBomItem;
import com.fabbitinc.server.domain.bom.repository.EngineeringBomItemRepository;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WhereUsedSummaryQuery {

    private final CurrentAuthProvider currentAuthProvider;
    private final EngineeringBomItemRepository engineeringBomItemRepository;
    private final PartRevisionRepository partRevisionRepository;
    private final PartRepository partRepository;

    public WhereUsedSummaryResult get(WhereUsedSummaryCondition condition) {
        currentAuthProvider.getCurrentAuth();

        PartRevision revision = partRevisionRepository.findById(condition.revisionId())
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "PartRevision '%s'을(를) 찾을 수 없습니다".formatted(condition.revisionId())
                ));

        List<EngineeringBomItem> bomItems = engineeringBomItemRepository
                .findByChildPartRevisionIdOrderByCreatedAtAsc(revision.getId());

        List<UUID> parentRevisionIds = bomItems.stream()
                .map(EngineeringBomItem::getParentPartRevisionId)
                .distinct()
                .toList();

        Map<UUID, PartRevision> parentRevisionsById = new LinkedHashMap<>();
        if (!parentRevisionIds.isEmpty()) {
            partRevisionRepository.findAllById(parentRevisionIds)
                    .forEach(r -> parentRevisionsById.put(r.getId(), r));
        }

        List<UUID> partIds = parentRevisionsById.values().stream()
                .map(PartRevision::getPartId)
                .distinct()
                .toList();
        Map<UUID, Part> partsById = new LinkedHashMap<>();
        if (!partIds.isEmpty()) {
            partRepository.findAllById(partIds)
                    .forEach(p -> partsById.put(p.getId(), p));
        }

        int draftCount = 0;
        int releasedCount = 0;
        int supersededCount = 0;
        int canceledCount = 0;

        List<WhereUsedSummaryResult.Reference> references = new ArrayList<>();
        for (UUID parentRevisionId : parentRevisionIds) {
            PartRevision parentRevision = parentRevisionsById.get(parentRevisionId);
            if (parentRevision == null) {
                continue;
            }
            Part part = partsById.get(parentRevision.getPartId());

            switch (parentRevision.getStatus()) {
                case DRAFT -> draftCount++;
                case RELEASED -> releasedCount++;
                case SUPERSEDED -> supersededCount++;
                case CANCELED -> canceledCount++;
            }

            references.add(new WhereUsedSummaryResult.Reference(
                    part == null ? null : part.getId(),
                    part == null ? null : part.getPartNumber(),
                    parentRevision.getName(),
                    parentRevision.getId(),
                    parentRevision.getRevisionCode(),
                    parentRevision.getStatus()
            ));
        }

        return new WhereUsedSummaryResult(
                parentRevisionIds.size(),
                new WhereUsedSummaryResult.StatusBreakdown(
                        draftCount, releasedCount, supersededCount, canceledCount
                ),
                references
        );
    }
}
