package com.fabbitinc.server.domain.project.repository;

import com.fabbitinc.server.domain.project.model.Project;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Optional<Project> findByIdAndDeletedFalse(UUID id);

    long countByDeletedFalse();

    @Query(
            value = """
                    select *
                    from projects p
                    where p.is_deleted = false
                      and (?1 is null or p.name ilike concat('%', ?1, '%'))
                    order by p.name
                    offset ?2
                    limit ?3
                    """,
            nativeQuery = true
    )
    List<Project> listProjectsPaginated(String search, int offset, int limit);

    @Query(
            value = """
                    select count(*)
                    from projects p
                    where p.is_deleted = false
                      and (?1 is null or p.name ilike concat('%', ?1, '%'))
                    """,
            nativeQuery = true
    )
    long countProjects(String search);

    @Query("""
            select p
            from Project p
            where p.deleted = false
              and lower(p.name) like lower(concat('%', ?1, '%'))
            order by p.name
            """)
    List<Project> findByNameContainingIgnoreCaseOrderByNameAsc(String name, Pageable pageable);

    List<Project> findByIdInAndDeletedFalseOrderByNameAsc(Collection<UUID> ids);
}
