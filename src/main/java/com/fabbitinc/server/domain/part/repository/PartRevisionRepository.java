package com.fabbitinc.server.domain.part.repository;

import com.fabbitinc.server.domain.part.model.PartRevision;
import com.fabbitinc.server.domain.part.model.PartRevisionStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PartRevisionRepository extends JpaRepository<PartRevision, UUID> {

    List<PartRevision> findByPartIdOrderByCreatedAtDesc(UUID partId);

    List<PartRevision> findByPartIdInOrderByCreatedAtDesc(Collection<UUID> partIds);

    Optional<PartRevision> findByIdAndPartId(UUID id, UUID partId);

    boolean existsByStatusIn(Collection<PartRevisionStatus> statuses);

    @Query(
            value = "select count(*) from part_revisions where jsonb_exists(extended_properties, ?1)",
            nativeQuery = true
    )
    long countByExtendedPropertiesContainingPropertyDefinitionId(String propertyDefinitionId);

    Optional<PartRevision> findByPartNumberAndRevisionCode(String partNumber, String revisionCode);
}
