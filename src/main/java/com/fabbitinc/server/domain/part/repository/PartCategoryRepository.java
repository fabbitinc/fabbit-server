package com.fabbitinc.server.domain.part.repository;

import com.fabbitinc.server.domain.part.model.PartCategory;
import com.fabbitinc.server.domain.part.model.PartItemType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartCategoryRepository extends JpaRepository<PartCategory, UUID> {

    Optional<PartCategory> findByName(String name);

    boolean existsByName(String name);

    boolean existsByPrefix(String prefix);

    List<PartCategory> findAllByOrderByNameAsc();

    List<PartCategory> findAllByItemTypeOrderByNameAsc(PartItemType itemType);
}
