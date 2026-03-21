package com.fabbitinc.server.domain.property.repository;

import com.fabbitinc.server.domain.property.model.PropertyDefinition;
import com.fabbitinc.server.domain.property.model.PropertyOwnerType;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyDefinitionRepository extends JpaRepository<PropertyDefinition, UUID> {

    boolean existsByOwnerTypeAndDisplayName(PropertyOwnerType ownerType, String displayName);

    boolean existsByOwnerTypeAndDisplayNameAndIdNot(
            PropertyOwnerType ownerType,
            String displayName,
            UUID id
    );

    List<PropertyDefinition> findByOwnerTypeAndActiveTrueOrderByDisplayOrderAscDisplayNameAsc(
            PropertyOwnerType ownerType
    );

    List<PropertyDefinition> findByOwnerTypeOrderByDisplayOrderAscDisplayNameAsc(
            PropertyOwnerType ownerType
    );

    List<PropertyDefinition> findByIdInAndOwnerType(
            Collection<UUID> ids,
            PropertyOwnerType ownerType
    );

    List<PropertyDefinition> findByIdInAndOwnerTypeAndActiveTrue(
            Collection<UUID> ids,
            PropertyOwnerType ownerType
    );
}
