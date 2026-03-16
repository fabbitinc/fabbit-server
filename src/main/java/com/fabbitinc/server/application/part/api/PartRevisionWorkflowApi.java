package com.fabbitinc.server.application.part.api;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.engineeringchange.api.EngineeringChangeApi;
import com.fabbitinc.server.application.engineeringchange.api.EngineeringChangeSnapshot;
import com.fabbitinc.server.application.part.service.PartRevisionRouteService;
import com.fabbitinc.server.application.part.service.PartRevisionService;
import com.fabbitinc.server.application.part.service.PartRevisionWorkflowPolicyService;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PartRevisionWorkflowApi {

    private final PartRevisionRepository partRevisionRepository;
    private final PartRevisionRouteService partRevisionRouteService;
    private final PartRevisionService partRevisionService;
    private final PartRevisionWorkflowPolicyService partRevisionWorkflowPolicyService;
    private final EngineeringChangeApi engineeringChangeApi;

    public DiffResult syncEngineeringChangePartRevisions(UUID engineeringChangeId, List<EngineeringChangePartRevisionRef> refs) {
        partRevisionWorkflowPolicyService.assertEngineeringChangeModeEnabled();
        List<PartRevision> currentLinks = partRevisionRepository.findByEngineeringChangeIdOrderByCreatedAtAsc(engineeringChangeId);
        Map<UUID, PartRevision> desiredById = resolveDrafts(refs);

        Set<UUID> currentIds = currentLinks.stream()
                .map(PartRevision::getId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<UUID> desiredIds = new LinkedHashSet<>(desiredById.keySet());

        Set<UUID> toAdd = new LinkedHashSet<>(desiredIds);
        toAdd.removeAll(currentIds);

        Set<UUID> toRemove = new LinkedHashSet<>(currentIds);
        toRemove.removeAll(desiredIds);

        Map<UUID, PartRevision> currentById = currentLinks.stream()
                .collect(java.util.stream.Collectors.toMap(
                        PartRevision::getId,
                        revision -> revision,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        List<PartRevision> addedRevisions = toAdd.stream()
                .map(desiredById::get)
                .toList();
        List<PartRevision> removedRevisions = toRemove.stream()
                .map(currentById::get)
                .filter(java.util.Objects::nonNull)
                .toList();

        for (UUID revisionId : toRemove) {
            currentLinks.stream()
                    .filter(revision -> revision.getId().equals(revisionId))
                    .findFirst()
                    .ifPresent(PartRevision::clearEngineeringChange);
        }

        for (UUID revisionId : toAdd) {
            PartRevision revision = desiredById.get(revisionId);
            if (revision.getEngineeringChangeId() != null && !engineeringChangeId.equals(revision.getEngineeringChangeId())) {
                throw new AppException(
                        ErrorCode.CONFLICT,
                        "이미 다른 변경관리에 연결된 초안입니다: %s/%s".formatted(
                                revision.getPartNumber(),
                                revision.getDraftKey()
                        )
                );
            }
            revision.assignEngineeringChange(engineeringChangeId);
        }

        Map<UUID, PartRevision> baseRevisions = resolveBaseRevisions(List.copyOf(union(currentLinks, addedRevisions)));
        return new DiffResult(
                toAdd.size(),
                toRemove.size(),
                addedRevisions.stream()
                        .map(revision -> toSnapshot(revision, baseRevisions.get(revision.getBaseRevisionId())))
                        .toList(),
                removedRevisions.stream()
                        .map(revision -> toSnapshot(revision, baseRevisions.get(revision.getBaseRevisionId())))
                        .toList()
        );
    }

    public List<EngineeringChangePartRevisionSnapshot> listEngineeringChangePartRevisions(UUID engineeringChangeId) {
        List<PartRevision> revisions = partRevisionRepository.findByEngineeringChangeIdOrderByCreatedAtAsc(engineeringChangeId);
        Map<UUID, PartRevision> baseRevisions = resolveBaseRevisions(revisions);
        return revisions.stream()
                .map(revision -> toSnapshot(revision, baseRevisions.get(revision.getBaseRevisionId())))
                .toList();
    }

    public void submitEngineeringChange(UUID actorId, UUID engineeringChangeId) {
        partRevisionWorkflowPolicyService.assertEngineeringChangeModeEnabled();
        EngineeringChangeSnapshot engineeringChange = engineeringChangeApi.getEngineeringChangeSnapshotMap(Set.of(engineeringChangeId))
                .get(engineeringChangeId);
        if (engineeringChange == null) {
            throw new AppException(ErrorCode.NOT_FOUND, "변경관리를 찾을 수 없습니다");
        }
        for (PartRevision revision : partRevisionRepository.findByEngineeringChangeIdOrderByCreatedAtAsc(engineeringChangeId)) {
            revision.assertDraftEditable();
        }
    }

    public void cancelEngineeringChange(UUID actorId, UUID engineeringChangeId, int engineeringChangeNumber, String engineeringChangeTitle) {
        partRevisionWorkflowPolicyService.assertEngineeringChangeModeEnabled();
        List<PartRevision> revisions = partRevisionRepository.findByEngineeringChangeIdOrderByCreatedAtAsc(engineeringChangeId).stream()
                .sorted(Comparator
                        .comparing(PartRevision::getPartNumber)
                        .thenComparing(revision -> revision.getDraftKey() == null ? "" : revision.getDraftKey()))
                .toList();
        for (PartRevision revision : revisions) {
            partRevisionService.cancelFromEngineeringChange(
                    revision,
                    actorId,
                    engineeringChangeId,
                    engineeringChangeNumber,
                    engineeringChangeTitle
            );
        }
    }

    public void releaseEngineeringChange(UUID actorId, UUID engineeringChangeId, int engineeringChangeNumber, String engineeringChangeTitle) {
        partRevisionWorkflowPolicyService.assertEngineeringChangeModeEnabled();
        List<PartRevision> revisions = partRevisionRepository.findByEngineeringChangeIdOrderByCreatedAtAsc(engineeringChangeId).stream()
                .sorted(Comparator
                        .comparing(PartRevision::getPartNumber)
                        .thenComparing(revision -> revision.getDraftKey() == null ? "" : revision.getDraftKey()))
                .toList();
        for (PartRevision revision : revisions) {
            partRevisionService.releaseDraftFromEngineeringChange(
                    revision,
                    actorId,
                    engineeringChangeId,
                    engineeringChangeNumber,
                    engineeringChangeTitle
            );
        }
    }

    private Map<UUID, PartRevision> resolveDrafts(List<EngineeringChangePartRevisionRef> refs) {
        Map<UUID, PartRevision> resolved = new LinkedHashMap<>();
        for (EngineeringChangePartRevisionRef ref : refs) {
            PartRevision revision = partRevisionRouteService.getRequiredDraft(
                    ref.partNumber(),
                    ref.baseRevisionCode(),
                    ref.draftKey()
            );
            resolved.putIfAbsent(revision.getId(), revision);
        }
        return resolved;
    }

    private Map<UUID, PartRevision> resolveBaseRevisions(List<PartRevision> revisions) {
        Set<UUID> baseRevisionIds = revisions.stream()
                .map(PartRevision::getBaseRevisionId)
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (baseRevisionIds.isEmpty()) {
            return Map.of();
        }
        return partRevisionRepository.findAllById(baseRevisionIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        PartRevision::getId,
                        revision -> revision,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private EngineeringChangePartRevisionSnapshot toSnapshot(PartRevision revision, PartRevision baseRevision) {
        return new EngineeringChangePartRevisionSnapshot(
                revision.getId(),
                revision.getPartId(),
                revision.getPartNumber(),
                baseRevision == null ? null : baseRevision.getRevisionCode(),
                revision.getDraftKey(),
                revision.getName(),
                revision.getStatus()
        );
    }

    private List<PartRevision> union(List<PartRevision> left, List<PartRevision> right) {
        List<PartRevision> result = new java.util.ArrayList<>(left.size() + right.size());
        result.addAll(left);
        result.addAll(right);
        return result;
    }

    public record DiffResult(
            int addedCount,
            int removedCount,
            List<EngineeringChangePartRevisionSnapshot> addedItems,
            List<EngineeringChangePartRevisionSnapshot> removedItems
    ) {
    }
}
