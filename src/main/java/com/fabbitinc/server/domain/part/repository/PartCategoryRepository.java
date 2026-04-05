package com.fabbitinc.server.domain.part.repository;

import com.fabbitinc.server.domain.part.model.PartCategory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartCategoryRepository extends JpaRepository<PartCategory, UUID> {

    Optional<PartCategory> findByName(String name);

    boolean existsByName(String name);

    boolean existsByFormatPrefixAndFormatSuffix(String formatPrefix, String formatSuffix);

    List<PartCategory> findAllByOrderByNameAsc();
}
