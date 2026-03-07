package com.fabbitinc.server.domain.part.repository;

import com.fabbitinc.server.domain.part.model.PartRevision;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartRevisionRepository extends JpaRepository<PartRevision, UUID> {

    List<PartRevision> findByPartIdOrderByCreatedAtDesc(UUID partId);
}
