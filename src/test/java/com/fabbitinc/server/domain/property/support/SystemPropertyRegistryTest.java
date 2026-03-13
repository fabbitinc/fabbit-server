package com.fabbitinc.server.domain.property.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fabbitinc.server.domain.property.model.PropertyOptionMode;
import com.fabbitinc.server.domain.property.model.PropertyOwnerType;
import com.fabbitinc.server.domain.property.model.PropertyValueType;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class SystemPropertyRegistryTest {

    @Test
    void systemPropertyRegistry는_ownerType과_propertyKey_조합이_중복되지_않는다() {
        Set<String> keys = SystemPropertyRegistry.list().stream()
                .map(spec -> spec.ownerType().name() + ":" + spec.propertyKey())
                .collect(Collectors.toSet());

        assertEquals(SystemPropertyRegistry.list().size(), keys.size());
    }

    @Test
    void part_category는_OPTION_CREATABLE이다() {
        SystemPropertySpec category = SystemPropertyRegistry.find(PropertyOwnerType.PART, "category")
                .orElseThrow();

        assertEquals(PropertyValueType.OPTION, category.valueType());
        assertEquals(PropertyOptionMode.CREATABLE, category.optionMode());
        assertEquals("category", category.columnName());
    }

    @Test
    void part_owner_spec은_존재한다() {
        assertTrue(SystemPropertyRegistry.find(PropertyOwnerType.PART, "part_number").isPresent());
    }
}
