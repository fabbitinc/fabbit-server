package com.fabbitinc.server.domain.supplier.repository;

import com.fabbitinc.server.domain.supplier.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    @Query(
            value = """
                    select s.*
                    from suppliers s
                    where (?1 is null
                        or s.company_name ilike concat('%', ?1, '%')
                        or coalesce(s.code, '') ilike concat('%', ?1, '%'))
                    order by s.company_name
                    offset ?2
                    limit ?3
                    """,
            nativeQuery = true
    )
    List<Supplier> listSuppliersPaginated(
            String search,
            int offset,
            int limit
    );

    @Query(
            value = """
                    select count(*)
                    from suppliers s
                    where (?1 is null
                        or s.company_name ilike concat('%', ?1, '%')
                        or coalesce(s.code, '') ilike concat('%', ?1, '%'))
                    """,
            nativeQuery = true
    )
    long countSuppliers(String search);
}
