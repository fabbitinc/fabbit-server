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
    void assignOwner_유효한_ID를_설정한다() {
        Part part = Part.create("P-001");
        UUID ownerId = UUID.randomUUID();

        part.assignOwner(ownerId);

        assertEquals(ownerId, part.getOwnerId());
    }

    @Test
    void assignOwner_null이면_예외를_던진다() {
        Part part = Part.create("P-001");

        DomainException ex = assertThrows(DomainException.class, () -> part.assignOwner(null));

        assertEquals(Part.CODE_PART_OWNER_REQUIRED, ex.getDomainCode());
    }

    @Test
    void unassignOwner_담당자를_비운다() {
        Part part = Part.create("P-001");
        part.assignOwner(UUID.randomUUID());

        part.unassignOwner();

        assertNull(part.getOwnerId());
    }

    @Test
    void assignOwnerTeam_유효한_ID를_설정한다() {
        Part part = Part.create("P-001");
        UUID ownerTeamId = UUID.randomUUID();

        part.assignOwnerTeam(ownerTeamId);

        assertEquals(ownerTeamId, part.getOwnerTeamId());
    }

    @Test
    void assignOwnerTeam_null이면_예외를_던진다() {
        Part part = Part.create("P-001");

        DomainException ex = assertThrows(DomainException.class, () -> part.assignOwnerTeam(null));

        assertEquals(Part.CODE_PART_OWNER_TEAM_REQUIRED, ex.getDomainCode());
    }

    @Test
    void unassignOwnerTeam_담당팀을_비운다() {
        Part part = Part.create("P-001");
        part.assignOwnerTeam(UUID.randomUUID());

        part.unassignOwnerTeam();

        assertNull(part.getOwnerTeamId());
    }

    @Test
    void changeLifecycleState와_clearLifecycleState로_수명주기상태를_변경한다() {
        Part part = Part.create("P-001");

        part.changeLifecycleState(PartLifecycleState.ACTIVE);
        assertEquals(PartLifecycleState.ACTIVE, part.getLifecycleState());

        part.clearLifecycleState();
        assertNull(part.getLifecycleState());
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
