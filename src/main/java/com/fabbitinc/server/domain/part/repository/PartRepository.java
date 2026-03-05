package com.fabbitinc.server.domain.part.repository;

import com.fabbitinc.server.domain.part.model.Part;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PartRepository extends JpaRepository<Part, UUID> {

    long countByCreatedAtGreaterThanEqual(Instant since);

    List<Part> findByPartNumberContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByPartNumberAsc(
            String partNumber,
            String name,
            Pageable pageable
    );

    List<Part> findAllByOrderByPartNumberAsc(Pageable pageable);

    List<Part> findTop20ByDrawingIdIsNullOrderByPartNumberAsc();

    long countByDrawingIdIsNull();

    List<Part> findByIdInOrderByPartNumberAsc(Collection<UUID> ids);

    List<Part> findByPartNumberIn(Collection<String> partNumbers);

    Optional<Part> findByPartNumber(String partNumber);

    @Query(
            "select p.category, count(p.id) " +
                    "from Part p " +
                    "where p.category is not null " +
                    "group by p.category " +
                    "order by p.category"
    )
    List<Object[]> findCategoryStats();

    @Query(
            "select distinct p.category " +
                    "from Part p " +
                    "where p.category is not null " +
                    "order by p.category"
    )
    List<String> findDistinctCategories();

    @Query(
            "select distinct p.lifecycleState " +
                    "from Part p " +
                    "where p.lifecycleState is not null " +
                    "order by p.lifecycleState"
    )
    List<String> findDistinctLifecycleStates();

    boolean existsByCategory(String category);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Part p set p.category = ?2 where p.category = ?1")
    int renameCategory(String oldName, String newName);
}
