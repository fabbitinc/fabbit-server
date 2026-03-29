package com.fabbitinc.server.application.bom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.bom.service.input.AddBomItemInput;
import com.fabbitinc.server.application.bom.service.input.AddBomItemsBatchInput;
import com.fabbitinc.server.application.bom.service.input.DeleteBomItemInput;
import com.fabbitinc.server.application.bom.service.input.UpdateBomItemInput;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.property.api.PropertyApi;
import com.fabbitinc.server.domain.bom.model.EngineeringBomItem;
import com.fabbitinc.server.domain.bom.repository.EngineeringBomItemRepository;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class EngineeringBomServiceTest {

    @Mock
    private EngineeringBomItemRepository engineeringBomItemRepository;
    @Mock
    private PartRevisionRepository partRevisionRepository;
    @Mock
    private PropertyApi propertyApi;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EngineeringBomService engineeringBomService;

    @Test
    void addBomItem_정상_추가한다() throws Exception {
        Part parentPart = Part.create("P-001");
        Part childPart = Part.create("P-002");
        PartRevision parentRevision = PartRevision.createInitialDraft(parentPart, "Parent", null);
        PartRevision childRevision = PartRevision.createOfficial(childPart, "1", null, "Child", PartRevisionStatus.RELEASED, null);

        when(partRevisionRepository.findByIdAndPartId(parentRevision.getId(), parentPart.getId()))
                .thenReturn(Optional.of(parentRevision));
        when(partRevisionRepository.findById(childRevision.getId()))
                .thenReturn(Optional.of(childRevision));
        when(partRevisionRepository.findById(parentRevision.getId()))
                .thenReturn(Optional.of(parentRevision));
        when(engineeringBomItemRepository.findByParentPartRevisionIdAndLineNumber(parentRevision.getId(), "10"))
                .thenReturn(Optional.empty());
        when(partRevisionRepository.findByPartIdOrderByCreatedAtDesc(childPart.getId()))
                .thenReturn(List.of(childRevision));
        when(engineeringBomItemRepository.findByParentPartRevisionIdOrderByCreatedAtAsc(childRevision.getId()))
                .thenReturn(List.of());
        when(propertyApi.validateExtendedProperties(any(), any())).thenReturn(Map.of());
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(engineeringBomItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        EngineeringBomItem result = engineeringBomService.addBomItem(new AddBomItemInput(
                parentPart.getId(),
                parentRevision.getId(),
                childRevision.getId(),
                "10",
                new BigDecimal("2"),
                Map.of()
        ));

        assertNotNull(result);
        assertEquals("10", result.getLineNumber());
    }

    @Test
    void addBomItem_순환참조가_감지되면_예외를_던진다() {
        // A → B → (A를 추가하려는 상황)
        Part partA = Part.create("P-A");
        Part partB = Part.create("P-B");
        PartRevision revA = PartRevision.createInitialDraft(partA, "PartA", null);
        PartRevision revB = PartRevision.createOfficial(partB, "1", null, "PartB", PartRevisionStatus.RELEASED, null);

        // revA가 DRAFT, revB를 자식으로 추가하려고 함
        when(partRevisionRepository.findByIdAndPartId(revA.getId(), partA.getId()))
                .thenReturn(Optional.of(revA));
        when(partRevisionRepository.findById(revB.getId()))
                .thenReturn(Optional.of(revB));
        when(engineeringBomItemRepository.findByParentPartRevisionIdAndLineNumber(revA.getId(), "10"))
                .thenReturn(Optional.empty());
        // BFS: revA 조회
        when(partRevisionRepository.findById(revA.getId()))
                .thenReturn(Optional.of(revA));
        // BFS: partB의 리비전 조회 → revB가 이미 partA를 자식으로 가지고 있음
        when(partRevisionRepository.findByPartIdOrderByCreatedAtDesc(partB.getId()))
                .thenReturn(List.of(revB));
        // revB의 BOM에 partA의 리비전이 자식으로 있음 (순환 구조)
        PartRevision revAOther = PartRevision.createOfficial(partA, "1", null, "PartA-Released", PartRevisionStatus.RELEASED, null);
        EngineeringBomItem bomBtoA = EngineeringBomItem.add(revB.getId(), "1", revAOther.getId(), BigDecimal.ONE, "{}");
        when(engineeringBomItemRepository.findByParentPartRevisionIdOrderByCreatedAtAsc(revB.getId()))
                .thenReturn(List.of(bomBtoA));
        when(partRevisionRepository.findById(revAOther.getId()))
                .thenReturn(Optional.of(revAOther));

        AppException ex = assertThrows(
                AppException.class,
                () -> engineeringBomService.addBomItem(new AddBomItemInput(
                        partA.getId(), revA.getId(), revB.getId(),
                        "10", BigDecimal.ONE, Map.of()
                ))
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
    }

    @Test
    void addBomItem_같은_Part의_다른_리비전이면_예외를_던진다() {
        Part part = Part.create("P-001");
        PartRevision revDraft = PartRevision.createInitialDraft(part, "Part", null);
        PartRevision revReleased = PartRevision.createOfficial(part, "1", null, "Part", PartRevisionStatus.RELEASED, null);

        when(partRevisionRepository.findByIdAndPartId(revDraft.getId(), part.getId()))
                .thenReturn(Optional.of(revDraft));
        when(partRevisionRepository.findById(revReleased.getId()))
                .thenReturn(Optional.of(revReleased));
        when(engineeringBomItemRepository.findByParentPartRevisionIdAndLineNumber(revDraft.getId(), "10"))
                .thenReturn(Optional.empty());
        when(partRevisionRepository.findById(revDraft.getId()))
                .thenReturn(Optional.of(revDraft));

        AppException ex = assertThrows(
                AppException.class,
                () -> engineeringBomService.addBomItem(new AddBomItemInput(
                        part.getId(), revDraft.getId(), revReleased.getId(),
                        "10", BigDecimal.ONE, Map.of()
                ))
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
    }

    @Test
    void addBomItem_lineNumber_중복이면_CONFLICT_예외를_던진다() {
        Part parentPart = Part.create("P-001");
        PartRevision parentRevision = PartRevision.createInitialDraft(parentPart, "Parent", null);
        PartRevision childRevision = PartRevision.createOfficial(Part.create("P-002"), "1", null, "Child", PartRevisionStatus.RELEASED, null);
        EngineeringBomItem existingItem = EngineeringBomItem.add(
                parentRevision.getId(), "10", UUID.randomUUID(), BigDecimal.ONE, "{}");

        when(partRevisionRepository.findByIdAndPartId(parentRevision.getId(), parentPart.getId()))
                .thenReturn(Optional.of(parentRevision));
        when(partRevisionRepository.findById(childRevision.getId()))
                .thenReturn(Optional.of(childRevision));
        when(engineeringBomItemRepository.findByParentPartRevisionIdAndLineNumber(parentRevision.getId(), "10"))
                .thenReturn(Optional.of(existingItem));

        AppException ex = assertThrows(
                AppException.class,
                () -> engineeringBomService.addBomItem(new AddBomItemInput(
                        parentPart.getId(),
                        parentRevision.getId(),
                        childRevision.getId(),
                        "10",
                        BigDecimal.ONE,
                        Map.of()
                ))
        );

        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
    }

    @Test
    void updateBomItem_수량을_변경한다() {
        Part parentPart = Part.create("P-001");
        PartRevision parentRevision = PartRevision.createInitialDraft(parentPart, "Parent", null);
        EngineeringBomItem item = EngineeringBomItem.add(
                parentRevision.getId(), "10", UUID.randomUUID(), BigDecimal.ONE, "{}");

        when(partRevisionRepository.findByIdAndPartId(parentRevision.getId(), parentPart.getId()))
                .thenReturn(Optional.of(parentRevision));
        when(engineeringBomItemRepository.findById(item.getId()))
                .thenReturn(Optional.of(item));

        engineeringBomService.updateBomItem(new UpdateBomItemInput(
                parentPart.getId(),
                parentRevision.getId(),
                item.getId(),
                null, false,
                null, false,
                new BigDecimal("5"), true,
                null, false
        ));

        assertEquals(0, new BigDecimal("5").compareTo(item.getQuantity()));
    }

    @Test
    void updateBomItem_존재하지않는_bomItem이면_NOT_FOUND() {
        Part parentPart = Part.create("P-001");
        PartRevision parentRevision = PartRevision.createInitialDraft(parentPart, "Parent", null);
        UUID nonExistentId = UUID.randomUUID();

        when(partRevisionRepository.findByIdAndPartId(parentRevision.getId(), parentPart.getId()))
                .thenReturn(Optional.of(parentRevision));
        when(engineeringBomItemRepository.findById(nonExistentId))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(
                AppException.class,
                () -> engineeringBomService.updateBomItem(new UpdateBomItemInput(
                        parentPart.getId(),
                        parentRevision.getId(),
                        nonExistentId,
                        null, false,
                        null, false,
                        BigDecimal.ONE, true,
                        null, false
                ))
        );

        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void deleteBomItem_정상_삭제한다() {
        Part parentPart = Part.create("P-001");
        PartRevision parentRevision = PartRevision.createInitialDraft(parentPart, "Parent", null);
        EngineeringBomItem item = EngineeringBomItem.add(
                parentRevision.getId(), "10", UUID.randomUUID(), BigDecimal.ONE, "{}");

        when(partRevisionRepository.findByIdAndPartId(parentRevision.getId(), parentPart.getId()))
                .thenReturn(Optional.of(parentRevision));
        when(engineeringBomItemRepository.findById(item.getId()))
                .thenReturn(Optional.of(item));

        engineeringBomService.deleteBomItem(new DeleteBomItemInput(
                parentPart.getId(), parentRevision.getId(), item.getId()));

        verify(engineeringBomItemRepository).delete(item);
    }

    @Test
    void addBomItemsBatch_500개_초과시_VALIDATION_ERROR() {
        List<AddBomItemsBatchInput.Item> items = new java.util.ArrayList<>();
        for (int i = 0; i < 501; i++) {
            items.add(new AddBomItemsBatchInput.Item(UUID.randomUUID(), String.valueOf(i), BigDecimal.ONE, Map.of()));
        }

        AppException ex = assertThrows(
                AppException.class,
                () -> engineeringBomService.addBomItemsBatch(new AddBomItemsBatchInput(
                        UUID.randomUUID(), UUID.randomUUID(), items))
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
    }

    @Test
    void addBomItemsBatch_입력내_lineNumber_중복시_VALIDATION_ERROR() {
        Part parentPart = Part.create("P-001");
        PartRevision parentRevision = PartRevision.createInitialDraft(parentPart, "Parent", null);

        when(partRevisionRepository.findByIdAndPartId(parentRevision.getId(), parentPart.getId()))
                .thenReturn(Optional.of(parentRevision));

        List<AddBomItemsBatchInput.Item> items = List.of(
                new AddBomItemsBatchInput.Item(UUID.randomUUID(), "10", BigDecimal.ONE, Map.of()),
                new AddBomItemsBatchInput.Item(UUID.randomUUID(), "10", BigDecimal.ONE, Map.of())
        );

        AppException ex = assertThrows(
                AppException.class,
                () -> engineeringBomService.addBomItemsBatch(new AddBomItemsBatchInput(
                        parentPart.getId(), parentRevision.getId(), items))
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
    }

    @Test
    void copyBomItems_기존_BOM을_새_리비전으로_복사한다() {
        UUID sourceRevisionId = UUID.randomUUID();
        UUID targetRevisionId = UUID.randomUUID();
        UUID childRevisionId = UUID.randomUUID();

        EngineeringBomItem sourceItem = EngineeringBomItem.add(
                sourceRevisionId, "10", childRevisionId, new BigDecimal("3"), "{\"a\":1}");

        when(engineeringBomItemRepository.findByParentPartRevisionIdOrderByCreatedAtAsc(sourceRevisionId))
                .thenReturn(List.of(sourceItem));
        when(engineeringBomItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        engineeringBomService.copyBomItems(sourceRevisionId, targetRevisionId);

        verify(engineeringBomItemRepository).saveAll(any());
    }

    @Test
    void copyBomItems_기존_BOM이_없으면_아무것도_하지_않는다() {
        UUID sourceRevisionId = UUID.randomUUID();
        UUID targetRevisionId = UUID.randomUUID();

        when(engineeringBomItemRepository.findByParentPartRevisionIdOrderByCreatedAtAsc(sourceRevisionId))
                .thenReturn(List.of());

        engineeringBomService.copyBomItems(sourceRevisionId, targetRevisionId);

        verify(engineeringBomItemRepository, never()).saveAll(any());
    }
}
