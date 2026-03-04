package com.fabbitinc.server.domain.part.repository;

import com.fabbitinc.server.domain.part.model.PartSupplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PartSupplierRepository extends JpaRepository<PartSupplier, UUID> {

    long countByPartId(UUID partId);

    List<PartSupplier> findByPartId(UUID partId);

    @Query("select count(distinct ps.partId) from PartSupplier ps")
    long countDistinctPartIds();
}
