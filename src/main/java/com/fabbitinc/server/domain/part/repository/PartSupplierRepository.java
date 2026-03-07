package com.fabbitinc.server.domain.part.repository;

import com.fabbitinc.server.domain.part.model.PartSupplier;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartSupplierRepository extends JpaRepository<PartSupplier, UUID> {

    long countByPartId(UUID partId);

    List<PartSupplier> findByPartId(UUID partId);

    Optional<PartSupplier> findByPartIdAndSupplierId(UUID partId, UUID supplierId);
}
