package com.fabbitinc.server.domain.bom.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.part.model.Part;
import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EngineeringBomItemTest {

    @Test
    void add_리비전과_줄번호를_저장한다() {
        Part parent = Part.create("P-001");
        Part child = Part.create("P-002");
        PartRevision parentRevision = PartRevision.createOfficial(parent, "1", null, "Parent", PartRevisionStatus.RELEASED, null);
        PartRevision childRevision = PartRevision.createOfficial(child, "1", null, "Child", PartRevisionStatus.RELEASED, null);

        EngineeringBomItem item = EngineeringBomItem.add(
                parentRevision.getId(),
                "10",
                childRevision.getId(),
                new BigDecimal("2.5"),
                "{}"
        );

        assertEquals(parentRevision.getId(), item.getParentPartRevisionId());
        assertEquals(childRevision.getId(), item.getChildPartRevisionId());
        assertEquals("10", item.getLineNumber());
        assertEquals(0, new BigDecimal("2.5").compareTo(item.getQuantity()));
    }

    @Test
    void add_수량이_0이하면_예외를_던진다() {
        DomainException ex = assertThrows(
                DomainException.class,
                () -> EngineeringBomItem.add(
                        UUID.randomUUID(),
                        "10",
                        UUID.randomUUID(),
                        BigDecimal.ZERO,
                        "{}"
                )
        );

        assertEquals(EngineeringBomItem.CODE_ENGINEERING_BOM_INVALID_QUANTITY, ex.getDomainCode());
    }

    @Test
    void add_같은_리비전으로_연결하면_예외를_던진다() {
        UUID revisionId = UUID.randomUUID();

        DomainException ex = assertThrows(
                DomainException.class,
                () -> EngineeringBomItem.add(
                        revisionId,
                        "10",
                        revisionId,
                        BigDecimal.ONE,
                        "{}"
                )
        );

        assertEquals(EngineeringBomItem.CODE_ENGINEERING_BOM_SELF_LINK_NOT_ALLOWED, ex.getDomainCode());
    }

    @Test
    void add_extendedProperties는_trim_정규화한다() {
        EngineeringBomItem item = EngineeringBomItem.add(
                UUID.randomUUID(),
                "10",
                UUID.randomUUID(),
                BigDecimal.ONE,
                "  {\"a\":1}  "
        );

        assertEquals("{\"a\":1}", item.getExtendedProperties());
    }

    @Test
    void changeQuantity_수량을_변경한다() {
        EngineeringBomItem item = EngineeringBomItem.add(
                UUID.randomUUID(),
                "10",
                UUID.randomUUID(),
                BigDecimal.ONE,
                "{}"
        );

        item.changeQuantity(new BigDecimal("3.25"));

        assertEquals(0, new BigDecimal("3.25").compareTo(item.getQuantity()));
    }

    @Test
    void changeExtendedProperties_blank면_기본_json으로_정규화한다() {
        EngineeringBomItem item = EngineeringBomItem.add(
                UUID.randomUUID(),
                "10",
                UUID.randomUUID(),
                BigDecimal.ONE,
                "{\"a\":1}"
        );

        item.changeExtendedProperties("   ");

        assertEquals("{}", item.getExtendedProperties());
    }
}
