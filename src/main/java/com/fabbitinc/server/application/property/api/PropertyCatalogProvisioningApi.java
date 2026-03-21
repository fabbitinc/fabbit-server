package com.fabbitinc.server.application.property.api;

import com.fabbitinc.server.domain.property.model.PropertyDefinition;
import com.fabbitinc.server.domain.property.model.PropertySourceType;
import com.fabbitinc.server.domain.property.repository.PropertyDefinitionRepository;
import com.fabbitinc.server.domain.property.support.DefaultSystemPropertyCatalog;
import com.fabbitinc.server.domain.property.support.SystemPropertyCatalogSeed;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PropertyCatalogProvisioningApi {

    private final PropertyDefinitionRepository propertyDefinitionRepository;

    public void syncSystemPropertyCatalog() {
        Map<String, PropertyDefinition> existingByCompositeKey = propertyDefinitionRepository
                .findBySourceType(PropertySourceType.SYSTEM)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        definition -> compositeKey(definition.getOwnerType().name(), definition.getPropertyKey()),
                        definition -> definition,
                        (left, right) -> right,
                        LinkedHashMap::new
                ));

        for (SystemPropertyCatalogSeed seed : DefaultSystemPropertyCatalog.items()) {
            String key = compositeKey(seed.ownerType().name(), seed.propertyKey());
            PropertyDefinition existing = existingByCompositeKey.get(key);
            if (existing == null) {
                propertyDefinitionRepository.save(PropertyDefinition.defineSystemProperty(
                        seed.ownerType(),
                        seed.propertyKey(),
                        seed.partSystemPropertyKind(),
                        seed.displayName(),
                        seed.description(),
                        seed.valueType(),
                        seed.optionMode(),
                        seed.options(),
                        seed.storageBinding(),
                        seed.displayOrder(),
                        seed.required(),
                        seed.activeConfigurable()
                ));
                continue;
            }

            existing.applySystemProvisioning(
                    seed.partSystemPropertyKind(),
                    seed.description(),
                    seed.valueType(),
                    seed.optionMode(),
                    seed.options(),
                    seed.storageBinding(),
                    seed.required(),
                    seed.activeConfigurable()
            );
        }
    }

    private String compositeKey(String ownerType, String propertyKey) {
        return ownerType + ":" + propertyKey;
    }
}
