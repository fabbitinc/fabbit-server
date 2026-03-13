package com.fabbitinc.server.domain.part.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fabbitinc.server.domain.common.exception.DomainException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PartRevisionTest {

    @Test
    void createInitial_파트의_품번을_스냅샷으로_보관한다() {
        Part part = Part.create("AES-100");

        PartRevision revision = PartRevision.createInitial(part, "1", "본체");

        assertEquals(part.getId(), revision.getPartId());
        assertEquals("AES-100", revision.getPartNumber());
        assertEquals("1", revision.getRevisionCode());
        assertEquals(PartRevisionStatus.DRAFT, revision.getStatus());
    }

    @Test
    void createDraft_baseRevisionId를_보관한다() {
        Part part = Part.create("AES-100");
        UUID baseRevisionId = UUID.randomUUID();

        PartRevision revision = PartRevision.createDraft(part, "2", baseRevisionId, "개정본");

        assertEquals(baseRevisionId, revision.getBaseRevisionId());
        assertEquals("AES-100", revision.getPartNumber());
        assertEquals("2", revision.getRevisionCode());
    }

    @Test
    void createInitial_파트가_null이면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> PartRevision.createInitial(null, "1", "본체"));

        assertEquals(PartRevision.CODE_PART_REVISION_PART_REQUIRED, ex.getDomainCode());
    }

    @Test
    void recordActivity_루트가_활동_이력을_직접_추가한다() {
        Part part = Part.create("AES-100");
        PartRevision revision = PartRevision.createInitial(part, "1", "본체");
        UUID actorId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-03-14T10:15:30Z");

        PartRevisionActivity activity = revision.recordActivityAt(
                actorId,
                PartRevisionActivityActionType.IMPORTED,
                PartRevisionActivitySourceType.SYNTHESIS,
                UUID.randomUUID(),
                "{\"file\":\"parts.xlsx\"}",
                occurredAt
        );

        assertEquals(1, revision.getActivities().size());
        assertEquals(revision.getId(), activity.getPartRevisionId());
        assertEquals(actorId, activity.getActorId());
        assertEquals(occurredAt, activity.getOccurredAt());
    }
}
