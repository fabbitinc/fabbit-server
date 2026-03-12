package com.fabbitinc.server.domain.property.repository;

import com.fabbitinc.server.domain.property.model.PropertyDefinition;
import com.fabbitinc.server.domain.property.model.PropertyOwnerType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyDefinitionRepository extends JpaRepository<PropertyDefinition, UUID> {

    Optional<PropertyDefinition> findByOwnerTypeAndPropertyKey(PropertyOwnerType ownerType, String propertyKey);

    boolean existsByOwnerTypeAndPropertyKey(PropertyOwnerType ownerType, String propertyKey);

    List<PropertyDefinition> findByOwnerTypeAndActiveTrueOrderByDisplayOrderAscDisplayNameAsc(
            PropertyOwnerType ownerType
    );
}
