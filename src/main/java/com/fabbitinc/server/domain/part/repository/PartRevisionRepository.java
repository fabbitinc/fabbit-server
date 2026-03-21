package com.fabbitinc.server.domain.part.repository;

import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PartRevisionRepository extends JpaRepository<PartRevision, UUID> {

    List<PartRevision> findByPartIdOrderByCreatedAtDesc(UUID partId);

    List<PartRevision> findByPartIdInOrderByCreatedAtDesc(Collection<UUID> partIds);

    Optional<PartRevision> findByIdAndPartId(UUID id, UUID partId);

    List<PartRevision> findByEngineeringChangeIdOrderByCreatedAtAsc(UUID engineeringChangeId);

    boolean existsByStatusIn(Collection<PartRevisionStatus> statuses);

    boolean existsByCategory(String category);

    long countByCategory(String category);

    @Query(
            value = "select count(*) from part_revisions where jsonb_exists(extended_properties, ?1)",
            nativeQuery = true
    )
    long countByExtendedPropertiesContainingPropertyDefinitionId(String propertyDefinitionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PartRevision pr set pr.category = ?2 where pr.category = ?1")
    int renameCategory(String oldName, String newName);

    @Query("select distinct pr.category from PartRevision pr where pr.category is not null order by pr.category asc")
    List<String> findDistinctCategories();
}
