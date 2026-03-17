package com.fabbitinc.server.domain.property.repository;

import com.fabbitinc.server.domain.property.model.PropertyOwnerType;
import com.fabbitinc.server.domain.property.model.SystemPropertyOverride;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemPropertyOverrideRepository extends JpaRepository<SystemPropertyOverride, UUID> {

    Optional<SystemPropertyOverride> findByOwnerTypeAndPropertyKey(PropertyOwnerType ownerType, String propertyKey);

    boolean existsByOwnerTypeAndPropertyKey(PropertyOwnerType ownerType, String propertyKey);

    List<SystemPropertyOverride> findByOwnerTypeAndActiveTrueOrderByDisplayOrderAscPropertyKeyAsc(
            PropertyOwnerType ownerType
    );

    List<SystemPropertyOverride> findByOwnerTypeOrderByDisplayOrderAscPropertyKeyAsc(
            PropertyOwnerType ownerType
    );
}
