package com.fabbitinc.server.domain.part.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.supplier.model.Supplier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PartSupplierTest {

    @Test
    void link_엔티티_입력시_연관과_ID를_동기화한다() {
        Part part = Part.create("P-001", "Bolt");
        Supplier supplier = Supplier.create("ACME", null, null, null, "{}");

        PartSupplier link = PartSupplier.link(part.getId(), supplier.getId(), 1200.0, "{}");

        assertEquals(part.getId(), link.getPartId());
        assertEquals(supplier.getId(), link.getSupplierId());
    }

    @Test
    void link_단가가_음수면_예외를_던진다() {
        Part part = Part.create("P-001", "Bolt");
        Supplier supplier = Supplier.create("ACME", null, null, null, "{}");

        DomainException ex = assertThrows(
                DomainException.class,
                () -> PartSupplier.link(part.getId(), supplier.getId(), -1.0, "{}")
        );

        assertEquals(PartSupplier.CODE_PART_SUPPLIER_UNIT_COST_INVALID, ex.getDomainCode());
    }

    @Test
    void link_extendedProperties는_trim_정규화한다() {
        Part part = Part.create("P-001", "Bolt");
        Supplier supplier = Supplier.create("ACME", null, null, null, "{}");

        PartSupplier link = PartSupplier.link(part.getId(), supplier.getId(), 1200.0, "  {\"key\":\"value\"}  ");

        assertEquals("{\"key\":\"value\"}", link.getExtendedProperties());
    }

    @Test
    void changeUnitCost_유효한값으로_단가를_변경한다() {
        PartSupplier link = PartSupplier.link(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), 1200.0, "{}");

        link.changeUnitCost(1500.0);

        assertEquals(1500.0, link.getUnitCost());
    }

    @Test
    void changeExtendedProperties_blank면_기본_json으로_정규화한다() {
        PartSupplier link = PartSupplier.link(java.util.UUID.randomUUID(), java.util.UUID.randomUUID(), 1200.0, "{}");

        link.changeExtendedProperties("   ");

        assertEquals("{}", link.getExtendedProperties());
    }
}
