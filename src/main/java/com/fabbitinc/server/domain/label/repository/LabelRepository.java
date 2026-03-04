package com.fabbitinc.server.domain.label.repository;

import com.fabbitinc.server.domain.label.model.Label;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LabelRepository extends JpaRepository<Label, UUID> {

    Optional<Label> findByName(String name);

    List<Label> findAllByOrderByNameAsc();

    @Query("""
            select l
            from Label l
            where (?1 is null or lower(l.name) like lower(concat('%', ?1, '%')))
            order by l.name
            """)
    List<Label> lookupLabels(String search, Pageable pageable);
}
