package com.fabbitinc.server.domain.property.repository;

import com.fabbitinc.server.domain.property.model.PropertyDefinition;
import com.fabbitinc.server.domain.property.model.PropertyOwnerType;
import com.fabbitinc.server.domain.property.model.PropertySourceType;
import com.fabbitinc.server.domain.property.model.PropertyStorageKind;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyDefinitionRepository extends JpaRepository<PropertyDefinition, UUID> {

    boolean existsByOwnerTypeAndDisplayName(PropertyOwnerType ownerType, String displayName);

    boolean existsByOwnerTypeAndDisplayNameAndIdNot(
            PropertyOwnerType ownerType,
            String displayName,
            UUID id
    );

    boolean existsByOwnerTypeAndPropertyKey(PropertyOwnerType ownerType, String propertyKey);

    Optional<PropertyDefinition> findByOwnerTypeAndPropertyKey(
            PropertyOwnerType ownerType,
            String propertyKey
    );

    List<PropertyDefinition> findByOwnerTypeAndActiveTrueOrderByDisplayOrderAscDisplayNameAsc(
            PropertyOwnerType ownerType
    );

    List<PropertyDefinition> findByOwnerTypeOrderByDisplayOrderAscDisplayNameAsc(
            PropertyOwnerType ownerType
    );

    List<PropertyDefinition> findByOwnerTypeAndPropertyKeyIn(
            PropertyOwnerType ownerType,
            Collection<String> propertyKeys
    );

    List<PropertyDefinition> findByOwnerTypeAndPropertyKeyInAndActiveTrueAndStorageKind(
            PropertyOwnerType ownerType,
            Collection<String> propertyKeys,
            PropertyStorageKind storageKind
    );

    List<PropertyDefinition> findByOwnerTypeAndSourceType(
            PropertyOwnerType ownerType,
            PropertySourceType sourceType
    );

    List<PropertyDefinition> findBySourceType(PropertySourceType sourceType);
}
