package com.fabbitinc.server.domain.part.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BomLinkTest {

    @Test
    void connect_엔티티_입력시_연관과_ID를_동기화한다() {
        Part parent = Part.create("P-001", "Parent");
        Part child = Part.create("P-002", "Child");

        BomLink link = BomLink.connect(parent, child, 2, "{}");

        assertEquals(parent, link.getParentPart());
        assertEquals(child, link.getChildPart());
        assertEquals(parent.getId(), link.getParentPartId());
        assertEquals(child.getId(), link.getChildPartId());
    }

    @Test
    void connect_수량이_0이하면_예외를_던진다() {
        UUID parentId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();

        DomainException ex = assertThrows(
                DomainException.class,
                () -> BomLink.connect(parentId, childId, 0, "{}")
        );

        assertEquals(BomLink.CODE_BOM_INVALID_QUANTITY, ex.getDomainCode());
    }

    @Test
    void connect_자기자신으로_연결하면_예외를_던진다() {
        UUID partId = UUID.randomUUID();

        DomainException ex = assertThrows(
                DomainException.class,
                () -> BomLink.connect(partId, partId, 1, "{}")
        );

        assertEquals(BomLink.CODE_BOM_SELF_LINK_NOT_ALLOWED, ex.getDomainCode());
    }

    @Test
    void connect_extendedProperties는_trim_정규화한다() {
        UUID parentId = UUID.randomUUID();
        UUID childId = UUID.randomUUID();

        BomLink link = BomLink.connect(parentId, childId, 1, "  {\"a\":1}  ");

        assertEquals("{\"a\":1}", link.getExtendedProperties());
    }
}
