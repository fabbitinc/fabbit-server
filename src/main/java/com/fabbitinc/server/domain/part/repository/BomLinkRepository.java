package com.fabbitinc.server.domain.part.repository;

import com.fabbitinc.server.domain.part.model.BomLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BomLinkRepository extends JpaRepository<BomLink, UUID> {

    long countByParentPartId(UUID parentPartId);

    long countByChildPartId(UUID childPartId);

    @Query("select distinct b.childPartId from BomLink b")
    List<UUID> findDistinctChildPartIds();

    @Query(
            value = """
                    select count(bl.id)
                    from bom_links bl
                    join parts p on p.id = bl.child_part_id
                    where p.name is null
                    """,
            nativeQuery = true
    )
    long countChildLinksWithUnnamedPart();

    Optional<BomLink> findByParentPartIdAndChildPartId(UUID parentPartId, UUID childPartId);
}
