package com.fabbitinc.server.application.engineeringchange.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.engineeringchange.service.EngineeringChangeService;
import com.fabbitinc.server.domain.bom.repository.EngineeringBomItemRepository;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChange;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeAffectedItem;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeAffectedItemType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeState;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * REVISION_RELEASE 영향 항목의 where-used(상위 어셈블리)를 조회하여
 * WHERE_USED_IMPACT 영향 항목으로 자동 추가하는 UseCase.
 * 사용자가 명시적으로 호출하는 populate 액션이다.
 */
@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class PopulateWhereUsedAffectedItemsUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final EngineeringChangeService engineeringChangeService;
    private final EngineeringBomItemRepository engineeringBomItemRepository;
    private final PartRevisionRepository partRevisionRepository;
    private final PartRepository partRepository;

    public PopulateResult execute(PopulateWhereUsedAffectedItemsCommand command) {
        currentAuthProvider.getCurrentAuth();

        EngineeringChange engineeringChange =
                engineeringChangeService.getEngineeringChangeByIdOrThrow(command.engineeringChangeId());

        if (engineeringChange.getState() != EngineeringChangeState.DRAFT) {
            throw new AppException(
                    ErrorCode.INVALID_STATE,
                    "DRAFT 상태의 EngineeringChange에서만 영향 항목을 추가할 수 있습니다"
            );
        }

        // 기존 affected items에서 REVISION_RELEASE 항목의 targetId(revisionId) 목록 수집
        List<EngineeringChangeAffectedItem> existingItems = engineeringChange.getAffectedItems();

        List<UUID> revisionReleaseTargetIds = existingItems.stream()
                .filter(item -> item.getItemType() == EngineeringChangeAffectedItemType.REVISION_RELEASE)
                .map(EngineeringChangeAffectedItem::getTargetId)
                .toList();

        if (revisionReleaseTargetIds.isEmpty()) {
            return new PopulateResult(List.of());
        }

        // 이미 등록된 targetId 집합 (중복 방지용)
        Set<UUID> existingTargetIds = existingItems.stream()
                .map(EngineeringChangeAffectedItem::getTargetId)
                .collect(Collectors.toSet());

        // 각 REVISION_RELEASE 대상 revision의 where-used 상위 part를 조회
        List<PopulatedItem> populatedItems = new ArrayList<>();

        for (UUID revisionId : revisionReleaseTargetIds) {
            try {
                List<PopulatedItem> items = findWhereUsedParents(
                        revisionId, existingTargetIds, engineeringChange
                );
                populatedItems.addAll(items);
            } catch (Exception ex) {
                log.warn(
                        "PartRevision '{}'의 where-used 조회 중 오류 발생. 건너뜁니다: {}",
                        revisionId,
                        ex.getMessage()
                );
            }
        }

        return new PopulateResult(populatedItems);
    }

    /**
     * 특정 revision의 where-used 상위 part revision을 조회하고,
     * 아직 등록되지 않은 항목을 EC에 WHERE_USED_IMPACT로 추가한다.
     */
    private List<PopulatedItem> findWhereUsedParents(
            UUID revisionId,
            Set<UUID> existingTargetIds,
            EngineeringChange engineeringChange
    ) {
        PartRevision revision = partRevisionRepository.findById(revisionId).orElse(null);
        if (revision == null) {
            log.warn("PartRevision '{}'을(를) 찾을 수 없습니다. 건너뜁니다.", revisionId);
            return List.of();
        }

        // where-used 조회: 이 revision을 사용하는 상위 BOM 항목 조회
        var bomItems = engineeringBomItemRepository
                .findByChildPartRevisionIdOrderByCreatedAtAsc(revision.getId());

        List<UUID> parentRevisionIds = bomItems.stream()
                .map(item -> item.getParentPartRevisionId())
                .distinct()
                .toList();

        if (parentRevisionIds.isEmpty()) {
            return List.of();
        }

        // 상위 revision 정보 조회
        Map<UUID, PartRevision> parentRevisionsById = new LinkedHashMap<>();
        partRevisionRepository.findAllById(parentRevisionIds)
                .forEach(r -> parentRevisionsById.put(r.getId(), r));

        // 상위 part 정보 조회
        List<UUID> partIds = parentRevisionsById.values().stream()
                .map(PartRevision::getPartId)
                .distinct()
                .toList();
        Map<UUID, Part> partsById = new LinkedHashMap<>();
        if (!partIds.isEmpty()) {
            partRepository.findAllById(partIds)
                    .forEach(p -> partsById.put(p.getId(), p));
        }

        List<PopulatedItem> result = new ArrayList<>();

        for (UUID parentRevisionId : parentRevisionIds) {
            // 이미 등록된 항목은 건너뛰기
            if (existingTargetIds.contains(parentRevisionId)) {
                continue;
            }

            PartRevision parentRevision = parentRevisionsById.get(parentRevisionId);
            if (parentRevision == null) {
                continue;
            }

            Part parentPart = partsById.get(parentRevision.getPartId());

            // EC에 WHERE_USED_IMPACT 영향 항목 추가
            engineeringChange.addAffectedItem(
                    EngineeringChangeAffectedItemType.WHERE_USED_IMPACT,
                    parentRevisionId,
                    null
            );

            // 중복 방지를 위해 집합에 추가
            existingTargetIds.add(parentRevisionId);

            result.add(new PopulatedItem(
                    parentRevisionId,
                    parentRevision.getRevisionCode(),
                    parentPart != null ? parentPart.getId() : null,
                    parentPart != null ? parentPart.getPartNumber() : null,
                    parentRevision.getName()
            ));
        }

        return result;
    }

    public record PopulateWhereUsedAffectedItemsCommand(
            UUID engineeringChangeId
    ) {
    }

    public record PopulateResult(
            List<PopulatedItem> populatedItems
    ) {
        public int count() {
            return populatedItems.size();
        }
    }

    public record PopulatedItem(
            UUID revisionId,
            String revisionCode,
            UUID partId,
            String partNumber,
            String partName
    ) {
    }
}
