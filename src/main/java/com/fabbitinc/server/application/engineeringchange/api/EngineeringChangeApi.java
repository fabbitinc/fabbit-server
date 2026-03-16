package com.fabbitinc.server.application.engineeringchange.api;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChange;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeIssueLink;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeIssueLinkRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeRepository;
import java.util.ArrayList;
import java.util.Collection;
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
public class EngineeringChangeApi {

    private final EngineeringChangeRepository engineeringChangeRepository;
    private final EngineeringChangeIssueLinkRepository engineeringChangeIssueLinkRepository;

    public UUID getEngineeringChangeIdByNumberOrThrow(int engineeringChangeNumber) {
        return engineeringChangeRepository.findByNumber(engineeringChangeNumber)
                .map(EngineeringChange::getId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "변경관리를 찾을 수 없습니다"));
    }

    public boolean existsEngineeringChange(UUID engineeringChangeId) {
        return engineeringChangeRepository.existsById(engineeringChangeId);
    }

    public int getNextEngineeringChangeNumberSeed() {
        return engineeringChangeRepository.findAllByOrderByNumberDesc(org.springframework.data.domain.PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(item -> item.getNumber() + 1)
                .orElse(1);
    }

    public void validateEngineeringChangeIds(Collection<UUID> engineeringChangeIds) {
        if (engineeringChangeIds == null) {
            return;
        }
        for (UUID engineeringChangeId : engineeringChangeIds) {
            if (engineeringChangeRepository.findById(engineeringChangeId).isEmpty()) {
                throw new AppException(ErrorCode.NOT_FOUND, "변경관리를 찾을 수 없습니다");
            }
        }
    }

    public Map<UUID, EngineeringChangeSnapshot> getEngineeringChangeSnapshotMap(Set<UUID> engineeringChangeIds) {
        if (engineeringChangeIds == null || engineeringChangeIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, EngineeringChangeSnapshot> result = new LinkedHashMap<>();
        engineeringChangeRepository.findAllById(engineeringChangeIds).forEach(change -> result.put(
                change.getId(),
                new EngineeringChangeSnapshot(
                        change.getId(),
                        change.getNumber(),
                        change.getTitle(),
                        change.getState()
                )
        ));
        return result;
    }

    public Map<UUID, List<EngineeringChangeSnapshot>> getLinkedEngineeringChangeSnapshotMap(Set<UUID> issueIds) {
        if (issueIds == null || issueIds.isEmpty()) {
            return Map.of();
        }

        List<EngineeringChangeIssueLink> links = engineeringChangeIssueLinkRepository.findByIssueIdIn(issueIds);
        Set<UUID> engineeringChangeIds = new LinkedHashSet<>();
        for (EngineeringChangeIssueLink link : links) {
            engineeringChangeIds.add(link.getEngineeringChangeId());
        }

        Map<UUID, EngineeringChangeSnapshot> snapshots = getEngineeringChangeSnapshotMap(engineeringChangeIds);
        Map<UUID, List<EngineeringChangeSnapshot>> result = new LinkedHashMap<>();
        for (EngineeringChangeIssueLink link : links) {
            EngineeringChangeSnapshot snapshot = snapshots.get(link.getEngineeringChangeId());
            if (snapshot == null) {
                continue;
            }
            result.computeIfAbsent(link.getIssueId(), ignored -> new ArrayList<>()).add(snapshot);
        }
        return result;
    }

    public DiffResult syncEngineeringChangesForIssue(
            UUID issueId,
            List<UUID> engineeringChangeIds
    ) {
        validateEngineeringChangeIds(engineeringChangeIds);

        Set<UUID> current = engineeringChangeIssueLinkRepository.findByIssueId(issueId).stream()
                .map(EngineeringChangeIssueLink::getEngineeringChangeId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<UUID> desired = new LinkedHashSet<>(engineeringChangeIds == null ? List.of() : engineeringChangeIds);

        Set<UUID> toAdd = new LinkedHashSet<>(desired);
        toAdd.removeAll(current);

        Set<UUID> toRemove = new LinkedHashSet<>(current);
        toRemove.removeAll(desired);

        if (!toRemove.isEmpty()) {
            engineeringChangeIssueLinkRepository.deleteByIssueIdAndEngineeringChangeIdIn(issueId, toRemove);
        }
        if (!toAdd.isEmpty()) {
            Map<UUID, EngineeringChange> engineeringChanges = new LinkedHashMap<>();
            engineeringChangeRepository.findAllById(toAdd)
                    .forEach(engineeringChange -> engineeringChanges.put(engineeringChange.getId(), engineeringChange));
            engineeringChangeIssueLinkRepository.saveAll(toAdd.stream()
                    .map(engineeringChangeId -> engineeringChanges.get(engineeringChangeId).linkIssue(issueId))
                    .toList());
        }

        return new DiffResult(toAdd, toRemove);
    }

    public record DiffResult(
            Set<UUID> added,
            Set<UUID> removed
    ) {
    }
}
