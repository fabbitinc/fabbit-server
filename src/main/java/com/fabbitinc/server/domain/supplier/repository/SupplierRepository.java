package com.fabbitinc.server.domain.supplier.repository;

import com.fabbitinc.server.domain.supplier.model.Supplier;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    List<Supplier> findAllByOrderByCompanyNameAsc(Pageable pageable);

    List<Supplier> findByCompanyNameContainingIgnoreCaseOrCodeContainingIgnoreCaseOrderByCompanyNameAsc(
            String companyName,
            String code,
            Pageable pageable
    );

    long countByCompanyNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String companyName, String code);

    @Query(
            value = "select count(*) from suppliers where jsonb_exists(extended_properties, :propertyDefinitionId)",
            nativeQuery = true
    )
    long countByExtendedPropertiesContainingPropertyDefinitionId(@Param("propertyDefinitionId") String propertyDefinitionId);

    Optional<Supplier> findByCompanyName(String companyName);
}
