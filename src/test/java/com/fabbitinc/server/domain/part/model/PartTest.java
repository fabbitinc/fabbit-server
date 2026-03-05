package com.fabbitinc.server.domain.part.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
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

        DomainException ex = assertThrows(DomainException.class, () -> part.assignDrawing(null));

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
