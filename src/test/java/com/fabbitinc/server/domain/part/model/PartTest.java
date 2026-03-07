package com.fabbitinc.server.domain.part.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fabbitinc.server.domain.common.exception.DomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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
    void changeCategory_trim_정규화한다() {
        Part part = Part.create("P-001", "Bolt");

        part.changeCategory("  FASTENER  ");

        assertEquals("FASTENER", part.getCategory());
    }

    @Test
    void changeCategory_빈문자열은_null로_정규화한다() {
        Part part = Part.create("P-001", "Bolt");

        part.changeCategory("   ");

        assertNull(part.getCategory());
    }

    @Test
    void changeCategory_길이초과면_예외를_던진다() {
        Part part = Part.create("P-001", "Bolt");

        DomainException ex = assertThrows(DomainException.class, () -> part.changeCategory("a".repeat(101)));

        assertEquals(Part.CODE_PART_CATEGORY_TOO_LONG, ex.getDomainCode());
    }

    @Test
    void changeMaterial_trim_정규화한다() {
        Part part = Part.create("P-001", "Bolt");

        part.changeMaterial("  AL6061  ");

        assertEquals("AL6061", part.getMaterial());
    }

    @Test
    void changeMaterial_빈문자열은_null로_정규화한다() {
        Part part = Part.create("P-001", "Bolt");

        part.changeMaterial("   ");

        assertNull(part.getMaterial());
    }

    @Test
    void changeMaterial_길이초과면_예외를_던진다() {
        Part part = Part.create("P-001", "Bolt");

        DomainException ex = assertThrows(DomainException.class, () -> part.changeMaterial("a".repeat(201)));

        assertEquals(Part.CODE_PART_MATERIAL_TOO_LONG, ex.getDomainCode());
    }

    @Test
    void changeUnit_trim_정규화한다() {
        Part part = Part.create("P-001", "Bolt");

        part.changeUnit("  EA  ");

        assertEquals("EA", part.getUnit());
    }

    @Test
    void changeUnit_빈문자열은_null로_정규화한다() {
        Part part = Part.create("P-001", "Bolt");

        part.changeUnit("   ");

        assertNull(part.getUnit());
    }

    @Test
    void changeUnit_길이초과면_예외를_던진다() {
        Part part = Part.create("P-001", "Bolt");

        DomainException ex = assertThrows(DomainException.class, () -> part.changeUnit("a".repeat(21)));

        assertEquals(Part.CODE_PART_UNIT_TOO_LONG, ex.getDomainCode());
    }

    @Test
    void changeDescription_trim_정규화한다() {
        Part part = Part.create("P-001", "Bolt");

        part.changeDescription("  sample desc  ");

        assertEquals("sample desc", part.getDescription());
    }

    @Test
    void changeDescription_빈문자열은_null로_정규화한다() {
        Part part = Part.create("P-001", "Bolt");

        part.changeDescription("   ");

        assertNull(part.getDescription());
    }

    @Test
    void changeExtendedProperties_blank이면_기본_json으로_정규화한다() {
        Part part = Part.create("P-001", "Bolt");

        part.changeExtendedProperties(" ");

        assertEquals("{}", part.getExtendedProperties());
    }

    @Test
    void changeExtendedProperties_trim_정규화한다() {
        Part part = Part.create("P-001", "Bolt");

        part.changeExtendedProperties("  {\"color\":\"silver\"}  ");

        assertEquals("{\"color\":\"silver\"}", part.getExtendedProperties());
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
    void assignOwner_null이면_예외를_던진다() {
        Part part = Part.create("P-001", "Bolt");

        DomainException ex = assertThrows(DomainException.class, () -> part.assignOwner(null));

        assertEquals(Part.CODE_PART_OWNER_REQUIRED, ex.getDomainCode());
    }

    @Test
    void assignOwnerTeam_유효한_ID를_설정한다() {
        Part part = Part.create("P-001", "Bolt");
        UUID ownerTeamId = UUID.randomUUID();

        part.assignOwnerTeam(ownerTeamId);

        assertEquals(ownerTeamId, part.getOwnerTeamId());
    }

    @Test
    void assignDrawing_유효한_ID를_설정한다() {
        Part part = Part.create("P-001", "Bolt");
        UUID drawingId = UUID.randomUUID();

        part.assignDrawing(drawingId);

        assertEquals(drawingId, part.getDrawingId());
    }

    @Test
    void markPhantom_markReal_clearPhantomFlag로_팬텀상태를_변경한다() {
        Part part = Part.create("P-001", "Bolt");

        part.markPhantom();
        assertEquals(Boolean.TRUE, part.getPhantom());

        part.markReal();
        assertEquals(Boolean.FALSE, part.getPhantom());

        part.clearPhantomFlag();
        assertNull(part.getPhantom());
    }

    @Test
    void changeLifecycleState와_clearLifecycleState로_수명주기상태를_변경한다() {
        Part part = Part.create("P-001", "Bolt");

        part.changeLifecycleState(PartLifecycleState.PRODUCTION);
        assertEquals(PartLifecycleState.PRODUCTION, part.getLifecycleState());

        part.clearLifecycleState();
        assertNull(part.getLifecycleState());
    }

    @Test
    void changeLeadTimeDays_음수면_예외를_던진다() {
        Part part = Part.create("P-001", "Bolt");

        DomainException ex = assertThrows(DomainException.class, () -> part.changeLeadTimeDays(-1));

        assertEquals(Part.CODE_PART_LEAD_TIME_DAYS_INVALID, ex.getDomainCode());
    }

    @Test
    void changeLeadTimeDays_null이면_값을_비운다() {
        Part part = Part.create("P-001", "Bolt");
        part.changeLeadTimeDays(3);

        part.changeLeadTimeDays(null);

        assertNull(part.getLeadTimeDays());
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
