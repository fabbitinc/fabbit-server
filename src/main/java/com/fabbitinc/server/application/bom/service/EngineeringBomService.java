package com.fabbitinc.server.application.bom.service;

import com.fabbitinc.server.application.bom.service.input.AddBomItemInput;
import com.fabbitinc.server.application.bom.service.input.AddBomItemsBatchInput;
import com.fabbitinc.server.application.bom.service.input.DeleteBomItemInput;
import com.fabbitinc.server.application.bom.service.input.UpdateBomItemInput;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.property.api.PropertyApi;
import com.fabbitinc.server.domain.bom.model.EngineeringBomItem;
import com.fabbitinc.server.domain.bom.repository.EngineeringBomItemRepository;
import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import com.fabbitinc.server.domain.property.model.PropertyOwnerType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class EngineeringBomService {

    private static final int MAX_BFS_DEPTH = 100;
    private static final int MAX_BATCH_SIZE = 500;

    private final EngineeringBomItemRepository engineeringBomItemRepository;
    private final PartRevisionRepository partRevisionRepository;
    private final PropertyApi propertyApi;
    private final ObjectMapper objectMapper;

    public EngineeringBomItem addBomItem(AddBomItemInput input) {
        requireEditableDraftRevision(input.partId(), input.revisionId());
        PartRevision childRevision = getRequiredRevision(input.childPartRevisionId());
        validateLineNumberNotDuplicated(input.revisionId(), input.lineNumber());
        validateNoCyclicReference(input.revisionId(), childRevision);

        String serializedProperties = serializeExtendedProperties(
                validateExtendedProperties(input.extendedProperties()));
        try {
            EngineeringBomItem item = EngineeringBomItem.add(
                    input.revisionId(),
                    input.lineNumber(),
                    input.childPartRevisionId(),
                    input.quantity(),
                    serializedProperties
            );
            return engineeringBomItemRepository.save(item);
        } catch (DomainException ex) {
            throw toBomAppException(ex);
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.CONFLICT,
                    "BOM 줄 번호 '%s'이(가) 이미 존재합니다".formatted(input.lineNumber()));
        }
    }

    public void updateBomItem(UpdateBomItemInput input) {
        requireEditableDraftRevision(input.partId(), input.revisionId());
        EngineeringBomItem item = getRequiredBomItem(input.revisionId(), input.bomItemId());

        if (!input.hasAnyFieldSet()) {
            return;
        }

        try {
            if (input.childPartRevisionIdSet()) {
                if (input.childPartRevisionId() == null) {
                    throw new AppException(ErrorCode.VALIDATION_ERROR, "하위 부품 리비전 ID는 null일 수 없습니다");
                }
                PartRevision childRevision = getRequiredRevision(input.childPartRevisionId());
                validateNoCyclicReference(input.revisionId(), childRevision);
                item.changeChildPartRevision(input.childPartRevisionId());
            }
            if (input.lineNumberSet()) {
                if (input.lineNumber() == null || input.lineNumber().isBlank()) {
                    throw new AppException(ErrorCode.VALIDATION_ERROR, "BOM 줄 번호는 비어있을 수 없습니다");
                }
                validateLineNumberNotDuplicatedExcluding(input.revisionId(), input.lineNumber(), item.getId());
                item.changeLineNumber(input.lineNumber());
            }
            if (input.quantitySet()) {
                if (input.quantity() == null) {
                    throw new AppException(ErrorCode.VALIDATION_ERROR, "수량은 null일 수 없습니다");
                }
                item.changeQuantity(input.quantity());
            }
            if (input.extendedPropertiesSet()) {
                String serialized = serializeExtendedProperties(
                        validateExtendedProperties(input.extendedProperties()));
                item.changeExtendedProperties(serialized);
            }
        } catch (DomainException ex) {
            throw toBomAppException(ex);
        }
    }

    public void deleteBomItem(DeleteBomItemInput input) {
        requireEditableDraftRevision(input.partId(), input.revisionId());
        EngineeringBomItem item = getRequiredBomItem(input.revisionId(), input.bomItemId());
        engineeringBomItemRepository.delete(item);
    }

    public List<EngineeringBomItem> addBomItemsBatch(AddBomItemsBatchInput input) {
        if (input.items().size() > MAX_BATCH_SIZE) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "일괄 등록은 최대 %d개까지 가능합니다".formatted(MAX_BATCH_SIZE));
        }

        requireEditableDraftRevision(input.partId(), input.revisionId());

        validateBatchLineNumbersUnique(input.revisionId(), input.items());

        List<UUID> childRevisionIds = input.items().stream()
                .map(AddBomItemsBatchInput.Item::childPartRevisionId)
                .distinct()
                .toList();
        Map<UUID, PartRevision> childRevisionMap = loadRevisionMap(childRevisionIds);

        PartRevision parentRevision = getRequiredRevision(input.revisionId());
        validateNoCyclicReferenceBatch(parentRevision, childRevisionMap, input.items());

        try {
            List<EngineeringBomItem> items = new ArrayList<>();
            for (AddBomItemsBatchInput.Item entry : input.items()) {
                String serialized = serializeExtendedProperties(
                        validateExtendedProperties(entry.extendedProperties()));
                items.add(EngineeringBomItem.add(
                        input.revisionId(),
                        entry.lineNumber(),
                        entry.childPartRevisionId(),
                        entry.quantity(),
                        serialized
                ));
            }
            return engineeringBomItemRepository.saveAll(items);
        } catch (DomainException ex) {
            throw toBomAppException(ex);
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.CONFLICT, "BOM 줄 번호가 중복되었습니다");
        }
    }

    public void copyBomItems(UUID sourceRevisionId, UUID targetRevisionId) {
        List<EngineeringBomItem> sourceItems = engineeringBomItemRepository
                .findByParentPartRevisionIdOrderByCreatedAtAsc(sourceRevisionId);
        if (sourceItems.isEmpty()) {
            return;
        }

        List<EngineeringBomItem> copiedItems = sourceItems.stream()
                .map(source -> EngineeringBomItem.add(
                        targetRevisionId,
                        source.getLineNumber(),
                        source.getChildPartRevisionId(),
                        source.getQuantity(),
                        source.getExtendedProperties()
                ))
                .toList();
        engineeringBomItemRepository.saveAll(copiedItems);
    }

    // --- 순환 참조 검증 (단건: BFS) ---

    private void validateNoCyclicReference(UUID parentRevisionId, PartRevision childRevision) {
        PartRevision parentRevision = getRequiredRevision(parentRevisionId);
        UUID parentPartId = parentRevision.getPartId();
        UUID childPartId = childRevision.getPartId();

        if (parentPartId.equals(childPartId)) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "같은 부품의 리비전을 하위 BOM으로 추가할 수 없습니다");
        }

        Queue<UUID> queue = new ArrayDeque<>();
        Set<UUID> visited = new HashSet<>();
        queue.add(childPartId);
        visited.add(childPartId);

        int depth = 0;
        while (!queue.isEmpty() && depth < MAX_BFS_DEPTH) {
            int levelSize = queue.size();
            for (int i = 0; i < levelSize; i++) {
                UUID currentPartId = queue.poll();
                Set<UUID> childPartIds = collectChildPartIds(currentPartId);
                if (childPartIds.contains(parentPartId)) {
                    throw new AppException(ErrorCode.VALIDATION_ERROR,
                            "순환 참조가 감지되었습니다. 이 BOM 항목을 추가하면 순환 구조가 만들어집니다");
                }
                for (UUID nextPartId : childPartIds) {
                    if (visited.add(nextPartId)) {
                        queue.add(nextPartId);
                    }
                }
            }
            depth++;
        }

        if (!queue.isEmpty()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "BOM 트리 깊이가 %d을(를) 초과하여 순환 참조 검증을 완료할 수 없습니다".formatted(MAX_BFS_DEPTH));
        }
    }

    private Set<UUID> collectChildPartIds(UUID partId) {
        List<PartRevision> revisions = partRevisionRepository.findByPartIdOrderByCreatedAtDesc(partId);
        Set<UUID> childPartIds = new HashSet<>();
        for (PartRevision revision : revisions) {
            List<EngineeringBomItem> bomItems = engineeringBomItemRepository
                    .findByParentPartRevisionIdOrderByCreatedAtAsc(revision.getId());
            for (EngineeringBomItem item : bomItems) {
                PartRevision childRevision = partRevisionRepository.findById(item.getChildPartRevisionId())
                        .orElse(null);
                if (childRevision != null) {
                    childPartIds.add(childRevision.getPartId());
                }
            }
        }
        return childPartIds;
    }

    // --- 순환 참조 검증 (일괄: 인메모리 DAG + 위상 정렬) ---

    private void validateNoCyclicReferenceBatch(
            PartRevision parentRevision,
            Map<UUID, PartRevision> childRevisionMap,
            List<AddBomItemsBatchInput.Item> newItems
    ) {
        UUID parentPartId = parentRevision.getPartId();

        // 새 항목들의 child Part ID 수집
        Set<UUID> newChildPartIds = new HashSet<>();
        for (AddBomItemsBatchInput.Item item : newItems) {
            PartRevision childRev = childRevisionMap.get(item.childPartRevisionId());
            if (childRev == null) {
                throw new AppException(ErrorCode.NOT_FOUND,
                        "하위 부품 리비전 '%s'을(를) 찾을 수 없습니다".formatted(item.childPartRevisionId()));
            }
            UUID childPartId = childRev.getPartId();
            if (childPartId.equals(parentPartId)) {
                throw new AppException(ErrorCode.VALIDATION_ERROR,
                        "같은 부품의 리비전을 하위 BOM으로 추가할 수 없습니다 (줄 번호: %s)".formatted(item.lineNumber()));
            }
            newChildPartIds.add(childPartId);
        }

        // 인메모리 그래프 구축
        Map<UUID, Set<UUID>> graph = new HashMap<>();

        // 새 항목: parentPartId → childPartIds
        graph.computeIfAbsent(parentPartId, k -> new HashSet<>()).addAll(newChildPartIds);

        // 기존 BOM에서 영향 받는 서브트리 로드 (BFS로 탐색)
        Queue<UUID> toLoad = new ArrayDeque<>(newChildPartIds);
        Set<UUID> loaded = new HashSet<>(newChildPartIds);
        loaded.add(parentPartId);

        int depth = 0;
        while (!toLoad.isEmpty() && depth < MAX_BFS_DEPTH) {
            int levelSize = toLoad.size();
            for (int i = 0; i < levelSize; i++) {
                UUID currentPartId = toLoad.poll();
                Set<UUID> existingChildren = collectChildPartIds(currentPartId);
                if (!existingChildren.isEmpty()) {
                    graph.computeIfAbsent(currentPartId, k -> new HashSet<>()).addAll(existingChildren);
                    for (UUID child : existingChildren) {
                        if (loaded.add(child)) {
                            toLoad.add(child);
                        }
                    }
                }
            }
            depth++;
        }

        // Kahn's algorithm (위상 정렬)
        Map<UUID, Integer> inDegree = new HashMap<>();
        Set<UUID> allNodes = new HashSet<>(graph.keySet());
        for (Set<UUID> children : graph.values()) {
            allNodes.addAll(children);
        }
        for (UUID node : allNodes) {
            inDegree.put(node, 0);
        }
        for (Map.Entry<UUID, Set<UUID>> entry : graph.entrySet()) {
            for (UUID child : entry.getValue()) {
                inDegree.merge(child, 1, Integer::sum);
            }
        }

        Queue<UUID> zeroInDegree = new ArrayDeque<>();
        for (Map.Entry<UUID, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                zeroInDegree.add(entry.getKey());
            }
        }

        int sortedCount = 0;
        while (!zeroInDegree.isEmpty()) {
            UUID node = zeroInDegree.poll();
            sortedCount++;
            Set<UUID> children = graph.getOrDefault(node, Set.of());
            for (UUID child : children) {
                int newDegree = inDegree.merge(child, -1, Integer::sum);
                if (newDegree == 0) {
                    zeroInDegree.add(child);
                }
            }
        }

        if (sortedCount < allNodes.size()) {
            throw new AppException(ErrorCode.VALIDATION_ERROR,
                    "순환 참조가 감지되었습니다. 일괄 추가 항목이 순환 구조를 만듭니다");
        }
    }

    // --- lineNumber 중복 검증 ---

    private void validateLineNumberNotDuplicated(UUID revisionId, String lineNumber) {
        engineeringBomItemRepository.findByParentPartRevisionIdAndLineNumber(revisionId, lineNumber)
                .ifPresent(existing -> {
                    throw new AppException(ErrorCode.CONFLICT,
                            "BOM 줄 번호 '%s'이(가) 이미 존재합니다".formatted(lineNumber));
                });
    }

    private void validateLineNumberNotDuplicatedExcluding(UUID revisionId, String lineNumber, UUID excludeItemId) {
        engineeringBomItemRepository.findByParentPartRevisionIdAndLineNumber(revisionId, lineNumber)
                .filter(existing -> !existing.getId().equals(excludeItemId))
                .ifPresent(existing -> {
                    throw new AppException(ErrorCode.CONFLICT,
                            "BOM 줄 번호 '%s'이(가) 이미 존재합니다".formatted(lineNumber));
                });
    }

    private void validateBatchLineNumbersUnique(UUID revisionId, List<AddBomItemsBatchInput.Item> items) {
        // 입력 내 중복 검증
        Set<String> seen = new HashSet<>();
        for (AddBomItemsBatchInput.Item item : items) {
            if (!seen.add(item.lineNumber())) {
                throw new AppException(ErrorCode.VALIDATION_ERROR,
                        "일괄 등록 요청 내에 중복된 줄 번호가 있습니다: '%s'".formatted(item.lineNumber()));
            }
        }

        // 기존 BOM과 충돌 검증
        List<EngineeringBomItem> existingItems = engineeringBomItemRepository
                .findByParentPartRevisionIdOrderByCreatedAtAsc(revisionId);
        Set<String> existingLineNumbers = existingItems.stream()
                .map(EngineeringBomItem::getLineNumber)
                .collect(Collectors.toSet());
        for (String lineNumber : seen) {
            if (existingLineNumbers.contains(lineNumber)) {
                throw new AppException(ErrorCode.CONFLICT,
                        "BOM 줄 번호 '%s'이(가) 이미 존재합니다".formatted(lineNumber));
            }
        }
    }

    // --- 헬퍼 ---

    private void requireEditableDraftRevision(UUID partId, UUID revisionId) {
        PartRevision revision = partRevisionRepository.findByIdAndPartId(revisionId, partId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "부품 리비전 '%s/%s'을(를) 찾을 수 없습니다".formatted(partId, revisionId)));
        if (revision.getStatus() != PartRevisionStatus.DRAFT) {
            throw new AppException(ErrorCode.INVALID_STATE,
                    "DRAFT 상태의 리비전에서만 BOM을 편집할 수 있습니다");
        }
    }

    private EngineeringBomItem getRequiredBomItem(UUID revisionId, UUID bomItemId) {
        return engineeringBomItemRepository.findById(bomItemId)
                .filter(item -> item.getParentPartRevisionId().equals(revisionId))
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "BOM 항목 '%s'을(를) 찾을 수 없습니다".formatted(bomItemId)));
    }

    private PartRevision getRequiredRevision(UUID revisionId) {
        return partRevisionRepository.findById(revisionId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "부품 리비전 '%s'을(를) 찾을 수 없습니다".formatted(revisionId)));
    }

    private Map<UUID, PartRevision> loadRevisionMap(List<UUID> revisionIds) {
        return partRevisionRepository.findAllById(revisionIds).stream()
                .collect(Collectors.toMap(PartRevision::getId, rev -> rev));
    }

    private Map<String, Object> validateExtendedProperties(Map<String, Object> extendedProperties) {
        return propertyApi.validateExtendedProperties(PropertyOwnerType.BOM_LINK, extendedProperties);
    }

    private String serializeExtendedProperties(Map<String, Object> properties) {
        try {
            return objectMapper.writeValueAsString(properties == null ? Map.of() : properties);
        } catch (JacksonException ex) {
            throw new AppException(ErrorCode.BAD_REQUEST, "extended_properties를 직렬화할 수 없습니다");
        }
    }

    private AppException toBomAppException(DomainException ex) {
        return switch (ex.getDomainCode()) {
            case EngineeringBomItem.CODE_ENGINEERING_BOM_PARENT_REQUIRED,
                    EngineeringBomItem.CODE_ENGINEERING_BOM_LINE_NUMBER_REQUIRED,
                    EngineeringBomItem.CODE_ENGINEERING_BOM_LINE_NUMBER_TOO_LONG,
                    EngineeringBomItem.CODE_ENGINEERING_BOM_CHILD_REQUIRED,
                    EngineeringBomItem.CODE_ENGINEERING_BOM_INVALID_QUANTITY ->
                    new AppException(ErrorCode.VALIDATION_ERROR, ex.getMessage());
            case EngineeringBomItem.CODE_ENGINEERING_BOM_SELF_LINK_NOT_ALLOWED ->
                    new AppException(ErrorCode.VALIDATION_ERROR, ex.getMessage());
            default ->
                    new AppException(ErrorCode.INVALID_STATE, ex.getMessage());
        };
    }
}
