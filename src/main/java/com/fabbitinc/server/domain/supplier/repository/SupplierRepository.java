package com.fabbitinc.server.domain.supplier.repository;

import com.fabbitinc.server.domain.supplier.model.Supplier;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    List<Supplier> findAllByOrderByCompanyNameAsc(Pageable pageable);

    List<Supplier> findByCompanyNameContainingIgnoreCaseOrCodeContainingIgnoreCaseOrderByCompanyNameAsc(
            String companyName,
            String code,
            Pageable pageable
    );

    long countByCompanyNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String companyName, String code);

    Optional<Supplier> findByCompanyName(String companyName);
}
