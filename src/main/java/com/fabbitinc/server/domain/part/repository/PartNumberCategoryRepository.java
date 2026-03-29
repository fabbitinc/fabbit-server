package com.fabbitinc.server.domain.part.repository;

import com.fabbitinc.server.domain.part.model.PartNumberCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartNumberCategoryRepository extends JpaRepository<PartNumberCategory, UUID> {

    Optional<PartNumberCategory> findByName(String name);

    boolean existsByName(String name);

    boolean existsByPrefix(String prefix);

    List<PartNumberCategory> findAllByOrderByNameAsc();
}
