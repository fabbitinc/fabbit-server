package com.fabbitinc.server.domain.part.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fabbitinc.server.domain.common.exception.DomainException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PartRevisionHistoryTest {

    @Test
    void record_이력을_기록한다() {
        Part part = Part.create("AES-100");
        PartRevision revision = PartRevision.createInitialDraft(part, "D1", "본체", null);
        UUID actorId = UUID.randomUUID();

        PartRevisionHistory history = PartRevisionHistory.record(
                revision,
                actorId,
                PartRevisionHistoryActionType.IMPORTED,
                PartRevisionHistorySourceType.SYNTHESIS,
                UUID.randomUUID(),
                "  {\"file\":\"parts.xlsx\"}  "
        );

        assertEquals(revision.getId(), history.getPartRevisionId());
        assertEquals(actorId, history.getActorId());
        assertEquals(PartRevisionHistoryActionType.IMPORTED, history.getActionType());
        assertEquals(PartRevisionHistorySourceType.SYNTHESIS, history.getSourceType());
        assertEquals("{\"file\":\"parts.xlsx\"}", history.getPayload());
    }

    @Test
    void recordAt_리비전이_null이면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> PartRevisionHistory.recordAt(
                null,
                UUID.randomUUID(),
                PartRevisionHistoryActionType.CREATED,
                PartRevisionHistorySourceType.UI,
                null,
                "{}",
                Instant.now()
        ));

        assertEquals(PartRevisionHistory.CODE_PART_REVISION_HISTORY_REVISION_REQUIRED, ex.getDomainCode());
    }

    @Test
    void recordAt_발생시각이_null이면_예외를_던진다() {
        Part part = Part.create("AES-100");
        PartRevision revision = PartRevision.createInitialDraft(part, "D1", "본체", null);

        DomainException ex = assertThrows(DomainException.class, () -> PartRevisionHistory.recordAt(
                revision,
                UUID.randomUUID(),
                PartRevisionHistoryActionType.CREATED,
                PartRevisionHistorySourceType.UI,
                null,
                "{}",
                null
        ));

        assertEquals(PartRevisionHistory.CODE_PART_REVISION_HISTORY_OCCURRED_AT_REQUIRED, ex.getDomainCode());
    }
}
