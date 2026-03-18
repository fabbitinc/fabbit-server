package com.fabbitinc.server.domain.property.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fabbitinc.server.domain.property.model.PropertyOwnerType;
import org.junit.jupiter.api.Test;

class SystemPropertyRegistryTest {

    @Test
    void listByOwnerType_PART의_모든_시스템속성은_partSystemPropertyKind를_가진다() {
        var items = SystemPropertyRegistry.listByOwnerType(PropertyOwnerType.PART);

        assertEquals(10, items.size());
        items.forEach(item -> assertNotNull(
                item.partSystemPropertyKind(),
                () -> "PART 시스템 속성 kind가 없습니다: " + item.propertyKey()
        ));
    }
}
