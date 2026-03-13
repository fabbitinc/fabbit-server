package com.fabbitinc.server.domain.part.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fabbitinc.server.domain.common.exception.DomainException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PartRevisionActivityTest {

    @Test
    void record_활동을_기록한다() {
        Part part = Part.create("AES-100");
        PartRevision revision = PartRevision.createInitial(part, "1", "본체");
        UUID actorId = UUID.randomUUID();

        PartRevisionActivity activity = PartRevisionActivity.record(
                revision,
                actorId,
                PartRevisionActivityActionType.IMPORTED,
                PartRevisionActivitySourceType.SYNTHESIS,
                UUID.randomUUID(),
                "  {\"file\":\"parts.xlsx\"}  "
        );

        assertEquals(revision.getId(), activity.getPartRevisionId());
        assertEquals(actorId, activity.getActorId());
        assertEquals(PartRevisionActivityActionType.IMPORTED, activity.getActionType());
        assertEquals(PartRevisionActivitySourceType.SYNTHESIS, activity.getSourceType());
        assertEquals("{\"file\":\"parts.xlsx\"}", activity.getPayload());
    }

    @Test
    void recordAt_리비전이_null이면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> PartRevisionActivity.recordAt(
                null,
                UUID.randomUUID(),
                PartRevisionActivityActionType.CREATED,
                PartRevisionActivitySourceType.UI,
                null,
                "{}",
                Instant.now()
        ));

        assertEquals(PartRevisionActivity.CODE_PART_REVISION_ACTIVITY_REVISION_REQUIRED, ex.getDomainCode());
    }

    @Test
    void recordAt_발생시각이_null이면_예외를_던진다() {
        Part part = Part.create("AES-100");
        PartRevision revision = PartRevision.createInitial(part, "1", "본체");

        DomainException ex = assertThrows(DomainException.class, () -> PartRevisionActivity.recordAt(
                revision,
                UUID.randomUUID(),
                PartRevisionActivityActionType.CREATED,
                PartRevisionActivitySourceType.UI,
                null,
                "{}",
                null
        ));

        assertEquals(PartRevisionActivity.CODE_PART_REVISION_ACTIVITY_OCCURRED_AT_REQUIRED, ex.getDomainCode());
    }
}
