package com.fabbitinc.server.domain.part.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.drawing.model.Drawing;
import com.fabbitinc.server.domain.team.model.Team;
import com.fabbitinc.server.domain.user.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PartTest {

    @Test
    void create_품번과_품명을_정규화한다() {
        Part part = Part.create("  P-001  ", "  Bolt  ");

        assertEquals("P-001", part.getPartNumber());
        assertEquals("Bolt", part.getName());
        assertEquals("1", part.getRevision());
    }

    @Test
    void create_빈_품번이면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> Part.create("   ", "A"));

        assertEquals(Part.CODE_PART_NUMBER_REQUIRED, ex.getDomainCode());
    }

    @Test
    void changeName_빈문자열은_null로_정규화한다() {
        Part part = Part.create("P-001", "Bolt");

        part.changeName("   ");

        assertNull(part.getName());
    }

    @Test
    void assignDrawing_null이면_예외를_던진다() {
        Part part = Part.create("P-001", "Bolt");

        DomainException ex = assertThrows(DomainException.class, () -> part.assignDrawing((UUID) null));

        assertEquals(Part.CODE_PART_DRAWING_REQUIRED, ex.getDomainCode());
    }

    @Test
    void assignOwner_유효한_ID를_설정한다() {
        Part part = Part.create("P-001", "Bolt");
        UUID ownerId = UUID.randomUUID();

        part.assignOwner(ownerId);

        assertEquals(ownerId, part.getOwnerId());
    }

    @Test
    void assignOwner_엔티티를_설정하면_연관과_ID를_동기화한다() {
        Part part = Part.create("P-001", "Bolt");
        User owner = new User("owner@example.com", "hashed", "Owner");

        part.assignOwner(owner);

        assertEquals(owner, part.getOwner());
        assertEquals(owner.getId(), part.getOwnerId());
    }

    @Test
    void assignOwnerTeam_엔티티를_설정하면_연관과_ID를_동기화한다() {
        Part part = Part.create("P-001", "Bolt");
        Team ownerTeam = new Team("설계팀", null, UUID.randomUUID());

        part.assignOwnerTeam(ownerTeam);

        assertEquals(ownerTeam, part.getOwnerTeam());
        assertEquals(ownerTeam.getId(), part.getOwnerTeamId());
    }

    @Test
    void assignDrawing_엔티티를_설정하면_연관과_ID를_동기화한다() {
        Part part = Part.create("P-001", "Bolt");
        Drawing drawing = new Drawing("DWG-001", "볼트 도면");

        part.assignDrawing(drawing);

        assertEquals(drawing, part.getDrawing());
        assertEquals(drawing.getId(), part.getDrawingId());
    }

    @Test
    void bumpRevision_숫자_리비전을_증분한다() {
        Part part = Part.create("P-001", "Bolt");

        part.bumpRevision();

        assertEquals("2", part.getRevision());
    }

    @Test
    void bumpRevision_Z는_AA로_증분한다() {
        Part part = Part.create("P-001", "Bolt");
        ReflectionTestUtils.setField(part, "revision", "Z");
        part.bumpRevision();
        assertEquals("AA", part.getRevision());
    }
}
