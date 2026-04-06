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
        PartRevision revision = PartRevision.createInitialDraft(part, "본체", null);
        UUID actorId = UUID.randomUUID();

        PartRevisionHistory history = PartRevisionHistory.record(
                revision,
                actorId,
                PartRevisionHistoryActionType.IMPORTED,
                PartRevisionCreationSourceType.SYNTHESIS,
                null,
                UUID.randomUUID(),
                "  synthesis import  "
        );

        assertEquals(revision.getId(), history.getPartRevisionId());
        assertEquals(actorId, history.getActorId());
        assertEquals(PartRevisionHistoryActionType.IMPORTED, history.getActionType());
        assertEquals(PartRevisionCreationSourceType.SYNTHESIS, history.getCreationSourceType());
        assertEquals("synthesis import", history.getReason());
    }

    @Test
    void recordAt_리비전이_null이면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> PartRevisionHistory.recordAt(
                null,
                UUID.randomUUID(),
                PartRevisionHistoryActionType.CREATED,
                PartRevisionCreationSourceType.USER,
                null,
                null,
                null,
                Instant.now()
        ));

        assertEquals(PartRevisionHistory.CODE_PART_REVISION_HISTORY_REVISION_REQUIRED, ex.getDomainCode());
    }

    @Test
    void recordAt_발생시각이_null이면_예외를_던진다() {
        Part part = Part.create("AES-100");
        PartRevision revision = PartRevision.createInitialDraft(part, "본체", null);

        DomainException ex = assertThrows(DomainException.class, () -> PartRevisionHistory.recordAt(
                revision,
                UUID.randomUUID(),
                PartRevisionHistoryActionType.CREATED,
                PartRevisionCreationSourceType.USER,
                null,
                null,
                null,
                null
        ));

        assertEquals(PartRevisionHistory.CODE_PART_REVISION_HISTORY_OCCURRED_AT_REQUIRED, ex.getDomainCode());
    }

    @Test
    void recordAt_release는_릴리즈워크플로가_필수다() {
        Part part = Part.create("AES-100");
        PartRevision revision = PartRevision.createInitialDraft(part, "본체", null);

        DomainException ex = assertThrows(DomainException.class, () -> PartRevisionHistory.recordAt(
                revision,
                UUID.randomUUID(),
                PartRevisionHistoryActionType.RELEASED,
                null,
                null,
                null,
                "개정",
                Instant.now()
        ));

        assertEquals(PartRevisionHistory.CODE_PART_REVISION_HISTORY_RELEASE_WORKFLOW_REQUIRED, ex.getDomainCode());
    }

    @Test
    void recordAt_cancel은_출처축을_기록할수없다() {
        Part part = Part.create("AES-100");
        PartRevision revision = PartRevision.createInitialDraft(part, "본체", null);

        DomainException ex = assertThrows(DomainException.class, () -> PartRevisionHistory.recordAt(
                revision,
                UUID.randomUUID(),
                PartRevisionHistoryActionType.CANCELED,
                PartRevisionCreationSourceType.USER,
                null,
                null,
                "폐기",
                Instant.now()
        ));

        assertEquals(PartRevisionHistory.CODE_PART_REVISION_HISTORY_SOURCE_AXIS_INVALID, ex.getDomainCode());
    }
}
