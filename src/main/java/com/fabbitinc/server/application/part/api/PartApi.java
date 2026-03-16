package com.fabbitinc.server.application.part.api;

import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PartApi {

    private final PartRepository partRepository;
    private final PartRevisionRepository partRevisionRepository;
    private final EntityManager entityManager;

    public boolean existsPart(UUID partId) {
        return partRepository.existsById(partId);
    }

    public List<PartSnapshot> searchPartSnapshots(String keyword, int limit) {
        int resolvedLimit = Math.max(limit, 1);
        String normalizedKeyword = keyword == null ? null : keyword.trim();

        if (normalizedKeyword == null || normalizedKeyword.isBlank()) {
            List<UUID> ids = partRepository.findAllByOrderByPartNumberAsc(PageRequest.of(0, resolvedLimit)).stream()
                    .map(Part::getId)
                    .toList();
            return getPartSnapshotsByIdsOrdered(ids);
        }

        Query query = entityManager.createNativeQuery(
                """
                        select distinct p.id, p.part_number
                        from parts p
                        left join part_revisions pr on pr.part_id = p.id
                        where lower(p.part_number) like :keyword
                           or lower(coalesce(pr.name, '')) like :keyword
                        order by p.part_number asc
                        limit :limit
                        """
        );
        query.setParameter("keyword", "%" + normalizedKeyword.toLowerCase() + "%");
        query.setParameter("limit", resolvedLimit);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        List<UUID> ids = rows.stream()
                .map(row -> (UUID) row[0])
                .toList();
        return getPartSnapshotsByIdsOrdered(ids);
    }

    public List<PartSnapshot> getPartSnapshotsByIdsOrdered(List<UUID> partIds) {
        if (partIds == null || partIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, Part> partsById = new LinkedHashMap<>();
        partRepository.findAllById(partIds).forEach(part -> partsById.put(part.getId(), part));
        Map<UUID, PartRevision> revisionsByPartId = resolveCurrentRevisions(partsById.values());

        return partIds.stream()
                .map(partsById::get)
                .filter(Objects::nonNull)
                .map(part -> toSnapshot(part, revisionsByPartId.get(part.getId())))
                .toList();
    }

    public Map<UUID, PartSnapshot> getPartSnapshotMap(Set<UUID> partIds) {
        return getPartSnapshotsByIdsOrdered(partIds == null ? List.of() : List.copyOf(partIds)).stream()
                .collect(Collectors.toMap(PartSnapshot::id, snapshot -> snapshot, (left, right) -> left, LinkedHashMap::new));
    }

    private Map<UUID, PartRevision> resolveCurrentRevisions(Collection<Part> parts) {
        if (parts.isEmpty()) {
            return Map.of();
        }

        Map<UUID, PartRevision> resolved = new LinkedHashMap<>();
        Map<UUID, Part> partsById = parts.stream()
                .collect(Collectors.toMap(Part::getId, part -> part, (left, right) -> left, LinkedHashMap::new));

        Set<UUID> prioritizedRevisionIds = parts.stream()
                .map(this::pickPrioritizedRevisionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, PartRevision> revisionsById = partRevisionRepository.findAllById(prioritizedRevisionIds).stream()
                .collect(Collectors.toMap(PartRevision::getId, revision -> revision, (left, right) -> left, LinkedHashMap::new));

        for (Part part : parts) {
            UUID prioritizedRevisionId = pickPrioritizedRevisionId(part);
            if (prioritizedRevisionId == null) {
                continue;
            }
            PartRevision revision = revisionsById.get(prioritizedRevisionId);
            if (revision != null) {
                resolved.put(part.getId(), revision);
            }
        }

        Set<UUID> unresolvedPartIds = partsById.keySet().stream()
                .filter(partId -> !resolved.containsKey(partId))
                .collect(Collectors.toSet());
        if (unresolvedPartIds.isEmpty()) {
            return resolved;
        }

        Map<UUID, UUID> latestRevisionIdsByPartId = findLatestRevisionIdsByPartId(unresolvedPartIds);
        Map<UUID, PartRevision> latestRevisionsById = partRevisionRepository.findAllById(latestRevisionIdsByPartId.values()).stream()
                .collect(Collectors.toMap(PartRevision::getId, revision -> revision, (left, right) -> left, LinkedHashMap::new));

        for (Map.Entry<UUID, UUID> entry : latestRevisionIdsByPartId.entrySet()) {
            PartRevision revision = latestRevisionsById.get(entry.getValue());
            if (revision != null) {
                resolved.put(entry.getKey(), revision);
            }
        }
        return resolved;
    }

    private Map<UUID, UUID> findLatestRevisionIdsByPartId(Set<UUID> partIds) {
        if (partIds.isEmpty()) {
            return Map.of();
        }

        Query query = entityManager.createNativeQuery(
                """
                        select ranked.part_id, ranked.id
                        from (
                            select
                                pr.part_id,
                                pr.id,
                                row_number() over (
                                    partition by pr.part_id
                                    order by pr.created_at desc, pr.id desc
                                ) as rn
                            from part_revisions pr
                            where pr.part_id in (:partIds)
                        ) ranked
                        where ranked.rn = 1
                        """
        );
        query.setParameter("partIds", List.copyOf(partIds));

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        Map<UUID, UUID> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put((UUID) row[0], (UUID) row[1]);
        }
        return result;
    }

    private UUID pickPrioritizedRevisionId(Part part) {
        return part.getCurrentReleasedRevisionId();
    }

    private PartSnapshot toSnapshot(Part part, PartRevision revision) {
        return new PartSnapshot(
                part.getId(),
                revision == null ? null : revision.getId(),
                part.getPartNumber(),
                revision == null ? null : revision.getName(),
                revision == null ? null : revision.getRevisionCode(),
                revision == null ? null : revision.getMaterial(),
                revision == null ? null : revision.getUnit(),
                revision == null ? null : revision.getDescription(),
                revision == null ? null : revision.getCategory(),
                part.getLifecycleState(),
                revision == null ? null : revision.getPhantom(),
                revision == null ? null : revision.getLeadTimeDays(),
                revision == null ? "{}" : revision.getExtendedProperties()
        );
    }
}
