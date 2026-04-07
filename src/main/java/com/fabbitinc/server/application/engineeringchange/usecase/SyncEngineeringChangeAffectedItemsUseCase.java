package com.fabbitinc.server.application.engineeringchange.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.engineeringchange.service.EngineeringChangeService;
import com.fabbitinc.server.application.part.service.PartRevisionWorkflowPolicyService;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChange;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeAffectedItem;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeAffectedItemType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeState;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeRepository;
import com.fabbitinc.server.domain.engineeringchange.repository.EngineeringChangeAffectedItemRepository;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Component
@Transactional
@RequiredArgsConstructor
public class SyncEngineeringChangeAffectedItemsUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final EngineeringChangeService engineeringChangeService;
    private final EngineeringChangeRepository engineeringChangeRepository;
    private final EngineeringChangeAffectedItemRepository affectedItemRepository;
    private final PartRevisionRepository partRevisionRepository;
    private final PartRepository partRepository;
    private final PartRevisionWorkflowPolicyService partRevisionWorkflowPolicyService;
    private final ObjectMapper objectMapper;

    public SyncResult execute(SyncEngineeringChangeAffectedItemsCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        partRevisionWorkflowPolicyService.assertEngineeringChangeModeEnabled();

        EngineeringChange engineeringChange =
                engineeringChangeService.getEngineeringChangeByIdOrThrow(command.engineeringChangeId());
        if (engineeringChange.getState() != EngineeringChangeState.DRAFT) {
            throw new AppException(
                    ErrorCode.INVALID_STATE,
                    "DRAFT 상태의 EngineeringChange에서만 영향 항목을 변경할 수 있습니다"
            );
        }

        // 기존 affected items 제거
        engineeringChange.clearAffectedItems();

        // 새 affected items 추가
        for (Item item : command.items()) {
            validateItem(engineeringChange, item);
            String actionDetail = buildActionDetail(item);
            engineeringChange.addAffectedItem(item.itemType(), item.targetId(), actionDetail);
        }

        return new SyncResult(command.items().size());
    }

    private void validateItem(EngineeringChange engineeringChange, Item item) {
        if (item.itemType() == EngineeringChangeAffectedItemType.REVISION_RELEASE) {
            PartRevision revision = partRevisionRepository.findById(item.targetId())
                    .filter(rev -> rev.getStatus() == PartRevisionStatus.DRAFT)
                    .orElseThrow(() -> new AppException(
                            ErrorCode.NOT_FOUND,
                            "DRAFT 상태의 PartRevision '%s'을(를) 찾을 수 없습니다".formatted(item.targetId())
                    ));
            assertNotLinkedToAnotherActiveEngineeringChange(engineeringChange.getId(), revision.getId());
        } else if (item.itemType() == EngineeringChangeAffectedItemType.LIFECYCLE_CHANGE) {
            partRepository.findById(item.targetId())
                    .orElseThrow(() -> new AppException(
                            ErrorCode.NOT_FOUND,
                            "Part '%s'을(를) 찾을 수 없습니다".formatted(item.targetId())
                    ));
            if (item.targetState() == null) {
                throw new AppException(ErrorCode.VALIDATION_ERROR, "lifecycle 변경 시 targetState는 필수입니다");
            }
        }
    }

    private void assertNotLinkedToAnotherActiveEngineeringChange(UUID currentEngineeringChangeId, UUID revisionId) {
        List<EngineeringChangeAffectedItem> links = affectedItemRepository.findByTargetIdAndItemTypeOrderByCreatedAtAsc(
                revisionId,
                EngineeringChangeAffectedItemType.REVISION_RELEASE
        );

        List<EngineeringChangeState> activeStates = List.of(
                EngineeringChangeState.DRAFT,
                EngineeringChangeState.REVIEW_PENDING,
                EngineeringChangeState.APPROVAL_PENDING,
                EngineeringChangeState.RELEASE_PENDING
        );

        engineeringChangeRepository.findAllById(
                        links.stream()
                                .map(EngineeringChangeAffectedItem::getEngineeringChangeId)
                                .filter(ecId -> !ecId.equals(currentEngineeringChangeId))
                                .distinct()
                                .toList()
                ).stream()
                .filter(ec -> activeStates.contains(ec.getState()))
                .findFirst()
                .ifPresent(ec -> {
                    throw new AppException(
                            ErrorCode.CONFLICT,
                            "해당 draft revision은 이미 다른 진행중 EC에 연결되어 있습니다: EC-%d".formatted(ec.getNumber())
                    );
                });
    }

    private String buildActionDetail(Item item) {
        if (item.itemType() == EngineeringChangeAffectedItemType.LIFECYCLE_CHANGE) {
            Part part = partRepository.findById(item.targetId()).orElse(null);
            try {
                return objectMapper.writeValueAsString(Map.of(
                        "targetState", item.targetState().name(),
                        "previousState", part != null ? part.getLifecycleState().name() : "ACTIVE"
                ));
            } catch (Exception ex) {
                throw new AppException(ErrorCode.BAD_REQUEST, "actionDetail를 직렬화할 수 없습니다");
            }
        }
        return null;
    }

    public record SyncEngineeringChangeAffectedItemsCommand(
            UUID engineeringChangeId,
            List<Item> items
    ) {
        public SyncEngineeringChangeAffectedItemsCommand {
            items = items == null ? List.of() : List.copyOf(items);
        }
    }

    public record Item(
            EngineeringChangeAffectedItemType itemType,
            UUID targetId,
            com.fabbitinc.server.domain.part.model.PartLifecycleState targetState
    ) {
    }

    public record SyncResult(int count) {
    }
}
