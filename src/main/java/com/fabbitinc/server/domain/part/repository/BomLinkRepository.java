package com.fabbitinc.server.domain.part.repository;

import com.fabbitinc.server.domain.part.model.BomLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BomLinkRepository extends JpaRepository<BomLink, UUID> {

    long countByParentPartId(UUID parentPartId);

    long countByChildPartId(UUID childPartId);

    Optional<BomLink> findByParentPartIdAndChildPartId(UUID parentPartId, UUID childPartId);
}
