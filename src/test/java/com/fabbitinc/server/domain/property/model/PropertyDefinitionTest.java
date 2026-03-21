package com.fabbitinc.server.domain.property.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fabbitinc.server.domain.common.exception.DomainException;
import com.fabbitinc.server.domain.property.support.PartSystemPropertyKind;
import java.util.List;
import org.junit.jupiter.api.Test;

class PropertyDefinitionTest {

    @Test
    void defineCustomProperty_통합_catalog_필드를_생성한다() {
        PropertyDefinition definition = PropertyDefinition.defineCustomProperty(
                PropertyOwnerType.PART,
                "  표면처리  ",
                "  부품 표면 처리  ",
                PropertyValueType.STRING,
                null,
                null,
                40,
                false
        );

        assertEquals(PropertyOwnerType.PART, definition.getOwnerType());
        assertEquals(definition.getId().toString(), definition.getPropertyKey());
        assertEquals(PropertySourceType.CUSTOM, definition.getSourceType());
        assertEquals(PropertyStorageKind.EXTENDED_PROPERTY, definition.getStorageKind());
        assertEquals(definition.getPropertyKey(), definition.getStorageBinding());
        assertEquals("표면처리", definition.getDisplayName());
        assertEquals("부품 표면 처리", definition.getDescription());
        assertEquals(true, definition.isActiveConfigurable());
    }

    @Test
    void defineSystemProperty_컬럼기반_catalog_필드를_생성한다() {
        PropertyDefinition definition = PropertyDefinition.defineSystemProperty(
                PropertyOwnerType.PART,
                "material",
                PartSystemPropertyKind.MATERIAL,
                "재질",
                "부품 재질",
                PropertyValueType.STRING,
                null,
                List.of(),
                "material",
                4,
                false,
                true
        );

        assertEquals("material", definition.getPropertyKey());
        assertEquals(PropertySourceType.SYSTEM, definition.getSourceType());
        assertEquals(PropertyStorageKind.COLUMN, definition.getStorageKind());
        assertEquals("material", definition.getStorageBinding());
        assertEquals(PartSystemPropertyKind.MATERIAL, definition.getPartSystemPropertyKind());
    }

    @Test
    void systemProperty는_타입과_필수여부를_변경할_수없다() {
        PropertyDefinition definition = PropertyDefinition.defineSystemProperty(
                PropertyOwnerType.PART,
                "material",
                PartSystemPropertyKind.MATERIAL,
                "재질",
                "부품 재질",
                PropertyValueType.STRING,
                null,
                List.of(),
                "material",
                4,
                false,
                true
        );

        DomainException ex = assertThrows(DomainException.class, () -> definition.markRequired());

        assertEquals(PropertyDefinition.CODE_PROPERTY_DEFINITION_SYSTEM_SHAPE_IMMUTABLE, ex.getDomainCode());
    }

    @Test
    void activeConfigurable이_false인_시스템속성은_비활성화할_수없다() {
        PropertyDefinition definition = PropertyDefinition.defineSystemProperty(
                PropertyOwnerType.PART,
                "part_number",
                PartSystemPropertyKind.PART_NUMBER,
                "품번",
                "부품의 고유 식별자",
                PropertyValueType.STRING,
                null,
                List.of(),
                "part_number",
                1,
                true,
                false
        );

        DomainException ex = assertThrows(DomainException.class, definition::deactivate);

        assertEquals(PropertyDefinition.CODE_PROPERTY_DEFINITION_SYSTEM_ACTIVE_NOT_CONFIGURABLE, ex.getDomainCode());
    }

    @Test
    void customProperty는_이름변경과_비활성화가_가능하다() {
        PropertyDefinition definition = PropertyDefinition.defineCustomProperty(
                PropertyOwnerType.SUPPLIER,
                "비고",
                null,
                PropertyValueType.STRING,
                null,
                null,
                10,
                true
        );

        definition.renameDisplayName("메모");
        definition.deactivate();

        assertEquals("메모", definition.getDisplayName());
        assertFalse(definition.isActive());
    }
}
