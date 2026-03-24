package com.fabbitinc.server.domain.part.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fabbitinc.server.domain.common.exception.DomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PartTest {

    @Test
    void create_품번을_정규화한다() {
        Part part = Part.create("  P-001  ");

        assertEquals("P-001", part.getPartNumber());
    }

    @Test
    void create_빈_품번이면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> Part.create("   "));

        assertEquals(Part.CODE_PART_NUMBER_REQUIRED, ex.getDomainCode());
    }

    @Test
    void create_기본_lifecycleState는_ACTIVE이다() {
        Part part = Part.create("P-001");

        assertEquals(PartLifecycleState.ACTIVE, part.getLifecycleState());
    }

    @Test
    void changeLifecycleState_ACTIVE에서_EOL로_전환한다() {
        Part part = Part.create("P-001");

        part.changeLifecycleState(PartLifecycleState.EOL);

        assertEquals(PartLifecycleState.EOL, part.getLifecycleState());
    }

    @Test
    void changeLifecycleState_ACTIVE에서_OBSOLETE로_전환한다() {
        Part part = Part.create("P-001");

        part.changeLifecycleState(PartLifecycleState.OBSOLETE);

        assertEquals(PartLifecycleState.OBSOLETE, part.getLifecycleState());
    }

    @Test
    void changeLifecycleState_EOL에서_OBSOLETE로_전환한다() {
        Part part = Part.create("P-001");
        part.changeLifecycleState(PartLifecycleState.EOL);

        part.changeLifecycleState(PartLifecycleState.OBSOLETE);

        assertEquals(PartLifecycleState.OBSOLETE, part.getLifecycleState());
    }

    @Test
    void changeLifecycleState_OBSOLETE에서_전환하면_예외를_던진다() {
        Part part = Part.create("P-001");
        part.changeLifecycleState(PartLifecycleState.OBSOLETE);

        DomainException ex = assertThrows(DomainException.class,
                () -> part.changeLifecycleState(PartLifecycleState.ACTIVE));

        assertEquals(Part.CODE_PART_LIFECYCLE_TRANSITION_INVALID, ex.getDomainCode());
    }

    @Test
    void changeLifecycleState_EOL에서_ACTIVE로_전환하면_예외를_던진다() {
        Part part = Part.create("P-001");
        part.changeLifecycleState(PartLifecycleState.EOL);

        DomainException ex = assertThrows(DomainException.class,
                () -> part.changeLifecycleState(PartLifecycleState.ACTIVE));

        assertEquals(Part.CODE_PART_LIFECYCLE_TRANSITION_INVALID, ex.getDomainCode());
    }

    @Test
    void resetLifecycleState_ACTIVE로_리셋한다() {
        Part part = Part.create("P-001");
        part.changeLifecycleState(PartLifecycleState.EOL);

        part.resetLifecycleState();

        assertEquals(PartLifecycleState.ACTIVE, part.getLifecycleState());
    }

    @Test
    void assertNotObsolete_OBSOLETE이면_예외를_던진다() {
        Part part = Part.create("P-001");
        part.changeLifecycleState(PartLifecycleState.OBSOLETE);

        DomainException ex = assertThrows(DomainException.class, part::assertNotObsolete);

        assertEquals(Part.CODE_PART_OBSOLETE, ex.getDomainCode());
    }

    @Test
    void assertNotObsolete_ACTIVE이면_통과한다() {
        Part part = Part.create("P-001");

        part.assertNotObsolete();
    }

    @Test
    void assignCurrentReleasedRevision_유효한_ID를_설정한다() {
        Part part = Part.create("P-001");
        UUID revisionId = UUID.randomUUID();

        part.assignCurrentReleasedRevision(revisionId);

        assertEquals(revisionId, part.getCurrentReleasedRevisionId());
    }

    @Test
    void assignCurrentReleasedRevision_null이면_예외를_던진다() {
        Part part = Part.create("P-001");

        DomainException ex = assertThrows(DomainException.class, () -> part.assignCurrentReleasedRevision(null));

        assertEquals(Part.CODE_PART_RELEASED_REVISION_REQUIRED, ex.getDomainCode());
    }

    @Test
    void clearCurrentReleasedRevision_릴리즈리비전을_비운다() {
        Part part = Part.create("P-001");
        part.assignCurrentReleasedRevision(UUID.randomUUID());

        part.clearCurrentReleasedRevision();

        assertNull(part.getCurrentReleasedRevisionId());
    }
}
