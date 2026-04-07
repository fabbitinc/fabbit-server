package com.fabbitinc.server.application.engineeringchange.api;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.part.service.PartLifecycleService;
import com.fabbitinc.server.application.part.service.PartRevisionService;
import com.fabbitinc.server.application.part.service.PartRevisionWorkflowPolicyService;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeAffectedItemRepository;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeAffectedItem;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeAffectedItemType;
import com.fabbitinc.server.domain.part.model.PartLifecycleState;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class EngineeringChangeAffectedItemApi {

    private final EngineeringChangeAffectedItemRepository affectedItemRepository;
    private final PartRevisionRepository partRevisionRepository;
    private final PartRevisionService partRevisionService;
    private final PartLifecycleService partLifecycleService;
    private final PartRevisionWorkflowPolicyService partRevisionWorkflowPolicyService;
    private final ObjectMapper objectMapper;

    public void validateAffectedItems(UUID actorId, UUID engineeringChangeId) {
        partRevisionWorkflowPolicyService.assertEngineeringChangeModeEnabled();
        List<EngineeringChangeAffectedItem> items = affectedItemRepository
                .findByEngineeringChangeIdOrderByCreatedAtAsc(engineeringChangeId);
        for (EngineeringChangeAffectedItem item : items) {
            if (item.getItemType() == EngineeringChangeAffectedItemType.REVISION_RELEASE) {
                PartRevision revision = getRequiredRevision(item.getTargetId());
                revision.assertDraftEditable();
            }
        }
    }

    public void releaseAffectedItems(UUID actorId, UUID engineeringChangeId) {
        partRevisionWorkflowPolicyService.assertEngineeringChangeModeEnabled();
        List<EngineeringChangeAffectedItem> items = affectedItemRepository
                .findByEngineeringChangeIdOrderByCreatedAtAsc(engineeringChangeId);

        List<EngineeringChangeAffectedItem> revisionItems = items.stream()
                .filter(item -> item.getItemType() == EngineeringChangeAffectedItemType.REVISION_RELEASE)
                .sorted(Comparator.comparing(item -> {
                    PartRevision rev = getRequiredRevision(item.getTargetId());
                    return rev.getPartNumber();
                }))
                .toList();
        for (EngineeringChangeAffectedItem item : revisionItems) {
            PartRevision revision = getRequiredRevision(item.getTargetId());
            partRevisionService.releaseDraftFromEngineeringChange(revision, actorId, engineeringChangeId);
        }

        List<EngineeringChangeAffectedItem> lifecycleItems = items.stream()
                .filter(item -> item.getItemType() == EngineeringChangeAffectedItemType.LIFECYCLE_CHANGE)
                .toList();
        for (EngineeringChangeAffectedItem item : lifecycleItems) {
            PartLifecycleState targetState = parseTargetState(item.getActionDetail());
            partLifecycleService.changeLifecycleStateFromEngineeringChange(
                    item.getTargetId(), targetState, actorId, engineeringChangeId
            );
        }
    }

    public void cancelAffectedItems(UUID actorId, UUID engineeringChangeId) {
        partRevisionWorkflowPolicyService.assertEngineeringChangeModeEnabled();
        List<EngineeringChangeAffectedItem> items = affectedItemRepository
                .findByEngineeringChangeIdOrderByCreatedAtAsc(engineeringChangeId);

        List<EngineeringChangeAffectedItem> revisionItems = items.stream()
                .filter(item -> item.getItemType() == EngineeringChangeAffectedItemType.REVISION_RELEASE)
                .sorted(Comparator.comparing(item -> {
                    PartRevision rev = getRequiredRevision(item.getTargetId());
                    return rev.getPartNumber();
                }))
                .toList();
        for (EngineeringChangeAffectedItem item : revisionItems) {
            PartRevision revision = getRequiredRevision(item.getTargetId());
            if (revision.getStatus() == PartRevisionStatus.DRAFT) {
                partRevisionService.cancelFromEngineeringChange(revision, actorId, engineeringChangeId);
            }
        }

        List<EngineeringChangeAffectedItem> lifecycleItems = items.stream()
                .filter(item -> item.getItemType() == EngineeringChangeAffectedItemType.LIFECYCLE_CHANGE)
                .toList();
        for (EngineeringChangeAffectedItem item : lifecycleItems) {
            PartLifecycleState previousState = parsePreviousState(item.getActionDetail());
            if (previousState != null) {
                partLifecycleService.revertLifecycleState(item.getTargetId(), previousState);
            }
        }
    }

    private PartRevision getRequiredRevision(UUID revisionId) {
        return partRevisionRepository.findById(revisionId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.NOT_FOUND,
                        "PartRevision '%s'을(를) 찾을 수 없습니다".formatted(revisionId)
                ));
    }

    private PartLifecycleState parseTargetState(String actionDetail) {
        try {
            var node = objectMapper.readTree(actionDetail);
            String value = node.get("targetState").asText();
            return PartLifecycleState.valueOf(value);
        } catch (Exception ex) {
            throw new AppException(ErrorCode.BAD_REQUEST, "actionDetail에서 targetState를 읽을 수 없습니다");
        }
    }

    private PartLifecycleState parsePreviousState(String actionDetail) {
        try {
            var node = objectMapper.readTree(actionDetail);
            var previousNode = node.get("previousState");
            if (previousNode == null || previousNode.isNull()) {
                return null;
            }
            return PartLifecycleState.valueOf(previousNode.asText());
        } catch (Exception ex) {
            return null;
        }
    }
}
