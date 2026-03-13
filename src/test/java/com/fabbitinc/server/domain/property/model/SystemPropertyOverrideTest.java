package com.fabbitinc.server.domain.property.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fabbitinc.server.domain.common.exception.DomainException;
import org.junit.jupiter.api.Test;

class SystemPropertyOverrideTest {

    @Test
    void create_입력값을_trim_정규화한다() {
        SystemPropertyOverride override = SystemPropertyOverride.create(
                PropertyOwnerType.PART,
                "  category  ",
                "  품목 분류  ",
                30,
                true
        );

        assertEquals(PropertyOwnerType.PART, override.getOwnerType());
        assertEquals("category", override.getPropertyKey());
        assertEquals("품목 분류", override.getDisplayNameOverride());
        assertEquals(30, override.getDisplayOrder());
    }

    @Test
    void create_propertyKey가_없으면_예외를_던진다() {
        DomainException ex = assertThrows(DomainException.class, () -> SystemPropertyOverride.create(
                PropertyOwnerType.PART,
                " ",
                null,
                null,
                true
        ));

        assertEquals(
                SystemPropertyOverride.CODE_SYSTEM_PROPERTY_OVERRIDE_PROPERTY_KEY_REQUIRED,
                ex.getDomainCode()
        );
    }

    @Test
    void changeDisplayOrder_음수면_예외를_던진다() {
        SystemPropertyOverride override = SystemPropertyOverride.create(
                PropertyOwnerType.PART,
                "category",
                null,
                null,
                true
        );

        DomainException ex = assertThrows(DomainException.class, () -> override.changeDisplayOrder(-1));

        assertEquals(
                SystemPropertyOverride.CODE_SYSTEM_PROPERTY_OVERRIDE_DISPLAY_ORDER_INVALID,
                ex.getDomainCode()
        );
    }

    @Test
    void clearDisplayNameOverride와_deactivate가_가능하다() {
        SystemPropertyOverride override = SystemPropertyOverride.create(
                PropertyOwnerType.PART,
                "category",
                "품목 분류",
                10,
                true
        );

        override.clearDisplayNameOverride();
        override.deactivate();

        assertNull(override.getDisplayNameOverride());
        assertFalse(override.isActive());
    }
}
