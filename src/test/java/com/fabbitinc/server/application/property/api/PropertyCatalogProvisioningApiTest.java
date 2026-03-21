package com.fabbitinc.server.application.property.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.domain.property.model.PropertyDefinition;
import com.fabbitinc.server.domain.property.model.PropertySourceType;
import com.fabbitinc.server.domain.property.model.PropertyValueType;
import com.fabbitinc.server.domain.property.repository.PropertyDefinitionRepository;
import com.fabbitinc.server.domain.property.support.DefaultSystemPropertyCatalog;
import com.fabbitinc.server.domain.property.support.PartSystemPropertyKind;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PropertyCatalogProvisioningApiTest {

    @Mock
    private PropertyDefinitionRepository propertyDefinitionRepository;

    @Test
    void syncSystemPropertyCatalog_없는_시스템속성은_생성한다() {
        when(propertyDefinitionRepository.findBySourceType(PropertySourceType.SYSTEM)).thenReturn(List.of());

        PropertyCatalogProvisioningApi api = new PropertyCatalogProvisioningApi(propertyDefinitionRepository);

        api.syncSystemPropertyCatalog();

        verify(propertyDefinitionRepository, times(DefaultSystemPropertyCatalog.items().size()))
                .save(any(PropertyDefinition.class));
    }

    @Test
    void syncSystemPropertyCatalog_기존_row는_구조필드만_동기화한다() {
        PropertyDefinition existing = PropertyDefinition.defineSystemProperty(
                com.fabbitinc.server.domain.property.model.PropertyOwnerType.PART,
                "material",
                PartSystemPropertyKind.MATERIAL,
                "사용자 정의 이름",
                "예전 설명",
                PropertyValueType.STRING,
                null,
                List.of(),
                "legacy_binding",
                99,
                false,
                false
        );
        when(propertyDefinitionRepository.findBySourceType(PropertySourceType.SYSTEM))
                .thenReturn(List.of(existing));

        PropertyCatalogProvisioningApi api = new PropertyCatalogProvisioningApi(propertyDefinitionRepository);

        api.syncSystemPropertyCatalog();

        assertEquals("사용자 정의 이름", existing.getDisplayName());
        assertEquals("부품 재질", existing.getDescription());
        assertEquals("material", existing.getStorageBinding());
        assertEquals(true, existing.isActiveConfigurable());
    }
}
