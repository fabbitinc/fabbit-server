package com.fabbitinc.server.domain.part.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fabbitinc.server.domain.common.exception.DomainException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PartRevisionTest {

    @Test
    void createInitial_파트의_품번을_스냅샷으로_보관한다() {
        Part part = Part.create("AES-100");
        UUID actorId = UUID.randomUUID();

        PartRevision revision = PartRevision.createInitialDraft(part, "본체", actorId);

        assertEquals(part.getId(), revision.getPartId());
        assertEquals("AES-100", revision.getPartNumber());
        assertEquals(null, revision.getRevisionCode());
        assertEquals(PartRevisionStatus.DRAFT, revision.getStatus());
        assertEquals(actorId, revision.getCreatedBy());
        assertEquals(actorId, revision.getUpdatedBy());
    }

    @Test
    void createDraft_baseRevisionId를_보관한다() {
        Part part = Part.create("AES-100");
        UUID baseRevisionId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        PartRevision revision = PartRevision.createDraft(part, baseRevisionId, "개정본", actorId);

        assertEquals(baseRevisionId, revision.getBaseRevisionId());
        assertEquals("AES-100", revision.getPartNumber());
        assertEquals(null, revision.getRevisionCode());
        assertEquals(actorId, revision.getCreatedBy());
        assertEquals(actorId, revision.getUpdatedBy());
    }

    @Test
    void editDraft_수행자가_있으면_updatedBy를_갱신한다() {
        Part part = Part.create("AES-100");
        UUID creatorId = UUID.randomUUID();
        UUID editorId = UUID.randomUUID();
        PartRevision revision = PartRevision.createInitialDraft(part, "본체", creatorId);

        revision.editDraft(new PartRevisionDraftChanges(
                null,
                false,
                null,
                false,
                null,
                false,
                null,
                false,
                null,
                false,
                null,
                false,
                null,
                false,
                null,
                false
        ), editorId);

        assertEquals(creatorId, revision.getCreatedBy());
        assertEquals(editorId, revision.getUpdatedBy());
    }

    @Test
    void createInitial_파트가_null이면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> PartRevision.createInitialDraft(null, "본체", null));

        assertEquals(PartRevision.CODE_PART_REVISION_PART_REQUIRED, ex.getDomainCode());
    }

    @Test
    void recordHistory_루트가_이력_엔트리를_직접_추가한다() {
        Part part = Part.create("AES-100");
        PartRevision revision = PartRevision.createInitialDraft(part, "본체", null);
        UUID actorId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-03-14T10:15:30Z");

        PartRevisionHistory history = revision.recordHistoryAt(
                actorId,
                PartRevisionHistoryActionType.IMPORTED,
                PartRevisionHistorySourceType.SYNTHESIS,
                UUID.randomUUID(),
                "{\"file\":\"parts.xlsx\"}",
                occurredAt
        );

        assertEquals(1, revision.getHistories().size());
        assertEquals(revision.getId(), history.getPartRevisionId());
        assertEquals(actorId, history.getActorId());
        assertEquals(occurredAt, history.getOccurredAt());
    }

    @Test
    void assertDraftEditable_DRAFT가_아니면_예외를_던진다() {
        Part part = Part.create("AES-100");
        PartRevision revision = PartRevision.createOfficial(part, "1", null, "본체", PartRevisionStatus.RELEASED, null);

        DomainException ex = assertThrows(DomainException.class, revision::assertDraftEditable);

        assertEquals(PartRevision.CODE_PART_REVISION_DRAFT_REQUIRED, ex.getDomainCode());
    }

    @Test
    void copyEditableFieldsFrom_기준_리비전의_본문을_복제한다() {
        Part part = Part.create("AES-100");
        PartRevision source = PartRevision.createInitialDraft(part, "원본", null);
        source.changeCategory("FRAME");
        source.changeMaterial("AL6061");
        source.changeUnit("EA");
        source.changeDescription("원본 설명");
        source.markPhantom();
        source.changeLeadTimeDays(7);
        source.changeExtendedProperties("{\"weight\":1.2}");

        PartRevision target = PartRevision.createDraft(part, source.getId(), "초안", null);
        target.copyEditableFieldsFrom(source);

        assertEquals("원본", target.getName());
        assertEquals("FRAME", target.getCategory());
        assertEquals("AL6061", target.getMaterial());
        assertEquals("EA", target.getUnit());
        assertEquals("원본 설명", target.getDescription());
        assertTrue(Boolean.TRUE.equals(target.getPhantom()));
        assertEquals(7, target.getLeadTimeDays());
        assertEquals("{\"weight\":1.2}", target.getExtendedProperties());
        assertEquals(source.getId(), target.getBaseRevisionId());
    }

    @Test
    void release_DRAFT를_공식_RELEASED_리비전으로_전환한다() {
        Part part = Part.create("AES-100");
        PartRevision revision = PartRevision.createInitialDraft(part, "본체", null);

        revision.release("1", null);

        assertEquals(PartRevisionStatus.RELEASED, revision.getStatus());
        assertEquals("1", revision.getRevisionCode());
    }

    @Test
    void markSuperseded_공식_리비전만_허용한다() {
        Part part = Part.create("AES-100");
        PartRevision revision = PartRevision.createInitialDraft(part, "본체", null);

        DomainException ex = assertThrows(DomainException.class, () -> revision.markSuperseded(null));

        assertEquals(PartRevision.CODE_PART_REVISION_SUPERSEDE_INVALID_STATE, ex.getDomainCode());
    }

    @Test
    void cancel_공식전_리비전을_CANCELED로_전환한다() {
        Part part = Part.create("AES-100");
        PartRevision revision = PartRevision.createInitialDraft(part, "본체", null);

        revision.cancel(null);

        assertEquals(PartRevisionStatus.CANCELED, revision.getStatus());
        assertEquals(null, revision.getRevisionCode());
    }
}
