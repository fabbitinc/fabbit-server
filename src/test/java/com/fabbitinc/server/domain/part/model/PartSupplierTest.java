package com.fabbitinc.server.domain.part.model;

import com.fabbitinc.server.domain.supplier.model.Supplier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PartSupplierTest {

    @Test
    void link_엔티티_입력시_연관과_ID를_동기화한다() {
        Part part = Part.create("P-001", "Bolt");
        Supplier supplier = new Supplier("ACME", null, null, null, "{}");

        PartSupplier link = PartSupplier.link(part, supplier, 1200.0, "{}");

        assertEquals(part, link.getPart());
        assertEquals(supplier, link.getSupplier());
        assertEquals(part.getId(), link.getPartId());
        assertEquals(supplier.getId(), link.getSupplierId());
    }
}
