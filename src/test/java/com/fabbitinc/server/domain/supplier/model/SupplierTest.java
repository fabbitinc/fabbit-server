package com.fabbitinc.server.domain.supplier.model;

import com.fabbitinc.server.domain.common.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SupplierTest {

    @Test
    void supplier_문자열_필드는_trim_정규화한다() {
        Supplier supplier = Supplier.create(
                "  ACME  ",
                "  A-1  ",
                "  KR  ",
                "  support@acme.com  ",
                "  {\"tier\":\"gold\"}  "
        );

        assertEquals("ACME", supplier.getCompanyName());
        assertEquals("A-1", supplier.getCode());
        assertEquals("KR", supplier.getCountry());
        assertEquals("support@acme.com", supplier.getContactInfo());
        assertEquals("{\"tier\":\"gold\"}", supplier.getExtendedProperties());
    }

    @Test
    void supplier_공급사명이_blank면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () ->
                Supplier.create("   ", null, null, null, "{}")
        );

        assertEquals(Supplier.CODE_SUPPLIER_COMPANY_NAME_REQUIRED, ex.getDomainCode());
    }

    @Test
    void supplier_선택_필드는_blank면_null로_정규화한다() {
        Supplier supplier = Supplier.create("ACME", "  ", "   ", "   ", " ");

        assertNull(supplier.getCode());
        assertNull(supplier.getCountry());
        assertNull(supplier.getContactInfo());
        assertEquals("{}", supplier.getExtendedProperties());
    }

    @Test
    void supplier_companyName이_너무_길면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () ->
                Supplier.create("a".repeat(201), null, null, null, "{}")
        );

        assertEquals(Supplier.CODE_SUPPLIER_COMPANY_NAME_TOO_LONG, ex.getDomainCode());
    }

    @Test
    void supplier_code가_너무_길면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () ->
                Supplier.create("ACME", "a".repeat(101), null, null, "{}")
        );

        assertEquals(Supplier.CODE_SUPPLIER_CODE_TOO_LONG, ex.getDomainCode());
    }

    @Test
    void supplier_country가_너무_길면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () ->
                Supplier.create("ACME", null, "a".repeat(101), null, "{}")
        );

        assertEquals(Supplier.CODE_SUPPLIER_COUNTRY_TOO_LONG, ex.getDomainCode());
    }
}
