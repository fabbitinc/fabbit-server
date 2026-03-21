package com.fabbitinc.server.domain.part.repository;

import com.fabbitinc.server.domain.part.model.PartSupplier;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PartSupplierRepository extends JpaRepository<PartSupplier, UUID> {

    long countByPartRevisionId(UUID partRevisionId);

    @Query(
            value = "select count(*) from part_suppliers where jsonb_exists(extended_properties, :propertyDefinitionId)",
            nativeQuery = true
    )
    long countByExtendedPropertiesContainingPropertyDefinitionId(@Param("propertyDefinitionId") String propertyDefinitionId);

    List<PartSupplier> findByPartRevisionId(UUID partRevisionId);

    Optional<PartSupplier> findByPartRevisionIdAndSupplierId(UUID partRevisionId, UUID supplierId);
}
