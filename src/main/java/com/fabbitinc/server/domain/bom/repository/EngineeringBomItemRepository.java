package com.fabbitinc.server.domain.bom.repository;

import com.fabbitinc.server.domain.bom.model.EngineeringBomItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EngineeringBomItemRepository extends JpaRepository<EngineeringBomItem, UUID> {

    long countByParentPartRevisionId(UUID parentPartRevisionId);

    long countByChildPartRevisionId(UUID childPartRevisionId);

    @Query(
            value = "select count(*) from engineering_bom_items where jsonb_exists(extended_properties, :propertyDefinitionId)",
            nativeQuery = true
    )
    long countByExtendedPropertiesContainingPropertyDefinitionId(@Param("propertyDefinitionId") String propertyDefinitionId);

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
