package com.fabbitinc.server.application.engineeringchange.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.engineeringchange.service.EngineeringChangeService;
import com.fabbitinc.server.domain.bom.model.EngineeringBomItem;
import com.fabbitinc.server.domain.bom.repository.EngineeringBomItemRepository;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChange;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeAffectedItemType;
import com.fabbitinc.server.domain.engineeringchange.model.EngineeringChangeState;
import com.fabbitinc.server.domain.organization.model.MembershipRole;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.repository.PartRepository;
import com.fabbitinc.server.domain.part.repository.PartRevisionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PopulateWhereUsedAffectedItemsUseCaseTest {

    @Mock
    private CurrentAuthProvider currentAuthProvider;
    @Mock
    private EngineeringChangeService engineeringChangeService;
    @Mock
    private EngineeringBomItemRepository engineeringBomItemRepository;
    @Mock
    private PartRevisionRepository partRevisionRepository;
    @Mock
    private PartRepository partRepository;

    @InjectMocks
    private PopulateWhereUsedAffectedItemsUseCase useCase;

    @Test
    void draftRevision의_baseRevision을_사용하는_상위리비전을_whereUsed로_추가한다() {
        UUID actorId = UUID.randomUUID();
        EngineeringChange engineeringChange = EngineeringChange.create(1, "EC", "본문", null, actorId);
        Part childPart = Part.create("TEST001");
        PartRevision releasedChildRevision = PartRevision.createOfficial(
                childPart,
                "1",
                null,
                "test001",
                com.fabbitinc.server.domain.part.model.PartRevisionStatus.RELEASED,
                actorId
        );
        PartRevision draftChildRevision = PartRevision.createDraft(childPart, releasedChildRevision.getId(), "test001", actorId);
        Part parentPart = Part.create("TEST003");
        PartRevision parentRevision = PartRevision.createOfficial(
                parentPart,
                "1",
                null,
                "test003",
                com.fabbitinc.server.domain.part.model.PartRevisionStatus.RELEASED,
                actorId
        );
        parentPart.assignCurrentReleasedRevision(parentRevision.getId());
        EngineeringBomItem bomItem = EngineeringBomItem.add(
                parentRevision.getId(),
                "1",
                releasedChildRevision.getId(),
                BigDecimal.ONE,
                "{}"
        );
        engineeringChange.addAffectedItem(
                EngineeringChangeAffectedItemType.REVISION_RELEASE,
                draftChildRevision.getId(),
                null
        );

        when(currentAuthProvider.getCurrentAuth()).thenReturn(new AuthContext(
                actorId,
                "test@example.com",
                UUID.randomUUID(),
                MembershipRole.OWNER
        ));
        when(engineeringChangeService.getEngineeringChangeByIdOrThrow(engineeringChange.getId())).thenReturn(engineeringChange);
        when(partRevisionRepository.findById(draftChildRevision.getId())).thenReturn(Optional.of(draftChildRevision));
        when(engineeringBomItemRepository.findByChildPartRevisionIdOrderByCreatedAtAsc(draftChildRevision.getId()))
                .thenReturn(List.of());
        when(engineeringBomItemRepository.findByChildPartRevisionIdOrderByCreatedAtAsc(releasedChildRevision.getId()))
                .thenReturn(List.of(bomItem));
        when(partRevisionRepository.findAllById(List.of(parentRevision.getId()))).thenReturn(List.of(parentRevision));
        when(partRepository.findAllById(List.of(parentPart.getId()))).thenReturn(List.of(parentPart));

        PopulateWhereUsedAffectedItemsUseCase.PopulateResult result = useCase.execute(
                new PopulateWhereUsedAffectedItemsUseCase.PopulateWhereUsedAffectedItemsCommand(engineeringChange.getId())
        );

        assertEquals(1, result.count());
        assertEquals(parentRevision.getId(), result.populatedItems().getFirst().revisionId());
        assertEquals(parentPart.getPartNumber(), result.populatedItems().getFirst().partNumber());
        assertEquals(
                1,
                engineeringChange.getAffectedItems().stream()
                        .filter(item -> item.getItemType() == EngineeringChangeAffectedItemType.WHERE_USED_IMPACT)
                        .count()
        );
    }

    @Test
    void 현재공식리비전이_아닌_parentRevision은_whereUsed에_포함하지않는다() {
        UUID actorId = UUID.randomUUID();
        EngineeringChange engineeringChange = EngineeringChange.create(2, "EC", "본문", null, actorId);
        Part childPart = Part.create("TEST001");
        PartRevision releasedChildRevision = PartRevision.createOfficial(
                childPart,
                "1",
                null,
                "test001",
                com.fabbitinc.server.domain.part.model.PartRevisionStatus.RELEASED,
                actorId
        );
        PartRevision draftChildRevision = PartRevision.createDraft(childPart, releasedChildRevision.getId(), "test001", actorId);

        Part canceledParentPart = Part.create("TEST0002");
        PartRevision canceledParentRevision = PartRevision.createDraft(canceledParentPart, null, "상위품", actorId);
        canceledParentRevision.cancel(actorId);

        Part releasedParentPart = Part.create("TEST0003");
        PartRevision releasedParentRevision = PartRevision.createOfficial(
                releasedParentPart,
                "1",
                null,
                "TEST-003",
                com.fabbitinc.server.domain.part.model.PartRevisionStatus.RELEASED,
                actorId
        );
        releasedParentPart.assignCurrentReleasedRevision(releasedParentRevision.getId());

        EngineeringBomItem canceledBomItem = EngineeringBomItem.add(
                canceledParentRevision.getId(),
                "1",
                releasedChildRevision.getId(),
                BigDecimal.ONE,
                "{}"
        );
        EngineeringBomItem releasedBomItem = EngineeringBomItem.add(
                releasedParentRevision.getId(),
                "2",
                releasedChildRevision.getId(),
                BigDecimal.ONE,
                "{}"
        );
        engineeringChange.addAffectedItem(
                EngineeringChangeAffectedItemType.REVISION_RELEASE,
                draftChildRevision.getId(),
                null
        );

        when(currentAuthProvider.getCurrentAuth()).thenReturn(new AuthContext(
                actorId,
                "test@example.com",
                UUID.randomUUID(),
                MembershipRole.OWNER
        ));
        when(engineeringChangeService.getEngineeringChangeByIdOrThrow(engineeringChange.getId())).thenReturn(engineeringChange);
        when(partRevisionRepository.findById(draftChildRevision.getId())).thenReturn(Optional.of(draftChildRevision));
        when(engineeringBomItemRepository.findByChildPartRevisionIdOrderByCreatedAtAsc(draftChildRevision.getId()))
                .thenReturn(List.of());
        when(engineeringBomItemRepository.findByChildPartRevisionIdOrderByCreatedAtAsc(releasedChildRevision.getId()))
                .thenReturn(List.of(canceledBomItem, releasedBomItem));
        when(partRevisionRepository.findAllById(List.of(canceledParentRevision.getId(), releasedParentRevision.getId())))
                .thenReturn(List.of(canceledParentRevision, releasedParentRevision));
        when(partRepository.findAllById(List.of(canceledParentPart.getId(), releasedParentPart.getId())))
                .thenReturn(List.of(canceledParentPart, releasedParentPart));

        PopulateWhereUsedAffectedItemsUseCase.PopulateResult result = useCase.execute(
                new PopulateWhereUsedAffectedItemsUseCase.PopulateWhereUsedAffectedItemsCommand(engineeringChange.getId())
        );

        assertEquals(1, result.count());
        assertEquals(releasedParentRevision.getId(), result.populatedItems().getFirst().revisionId());
    }
}
