package com.fabbitinc.server.domain.bom.repository;

import com.fabbitinc.server.domain.bom.model.EngineeringBomItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EngineeringBomItemRepository extends JpaRepository<EngineeringBomItem, UUID> {

    long countByParentPartRevisionId(UUID parentPartRevisionId);

    long countByChildPartRevisionId(UUID childPartRevisionId);

    boolean existsByParentPartRevisionId(UUID parentPartRevisionId);

    Optional<EngineeringBomItem> findByParentPartRevisionIdAndLineNumber(UUID parentPartRevisionId, String lineNumber);

    List<EngineeringBomItem> findByParentPartRevisionIdOrderByCreatedAtAsc(UUID parentPartRevisionId);

    List<EngineeringBomItem> findByParentPartRevisionIdInOrderByParentPartRevisionIdAscCreatedAtAsc(
            List<UUID> parentPartRevisionIds
    );

    List<EngineeringBomItem> findByChildPartRevisionIdOrderByCreatedAtAsc(UUID childPartRevisionId);

    List<EngineeringBomItem> findByParentPartRevisionIdAndChildPartRevisionIdOrderByCreatedAtAsc(
            UUID parentPartRevisionId,
            UUID childPartRevisionId
    );
}
