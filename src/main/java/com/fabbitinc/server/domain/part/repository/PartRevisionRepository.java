package com.fabbitinc.server.domain.part.repository;

import com.fabbitinc.server.domain.part.model.PartRevision;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PartRevisionRepository extends JpaRepository<PartRevision, UUID> {

    List<PartRevision> findByPartIdOrderByCreatedAtDesc(UUID partId);

    List<PartRevision> findByPartIdInOrderByCreatedAtDesc(Collection<UUID> partIds);

    List<PartRevision> findByPartNumberOrderByCreatedAtDesc(String partNumber);

    List<PartRevision> findByPartNumberContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByPartNumberAscCreatedAtDesc(
            String partNumber,
            String name,
            Pageable pageable
    );

    Optional<PartRevision> findByPartNumberAndRevisionCode(String partNumber, String revisionCode);

    Optional<PartRevision> findByPartNumberAndDraftKey(String partNumber, String draftKey);

    Optional<PartRevision> findByPartNumberAndDraftKeyAndBaseRevisionId(String partNumber, String draftKey, UUID baseRevisionId);

    Optional<PartRevision> findByPartNumberAndDraftKeyAndBaseRevisionIdIsNull(String partNumber, String draftKey);

    List<PartRevision> findByChangeRequestIdOrderByCreatedAtAsc(UUID changeRequestId);

    boolean existsByCategory(String category);

    long countByCategory(String category);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PartRevision pr set pr.category = ?2 where pr.category = ?1")
    int renameCategory(String oldName, String newName);

    @Query("select distinct pr.category from PartRevision pr where pr.category is not null order by pr.category asc")
    List<String> findDistinctCategories();
}
