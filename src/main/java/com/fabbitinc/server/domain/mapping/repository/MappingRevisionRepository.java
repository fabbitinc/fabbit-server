package com.fabbitinc.server.domain.mapping.repository;

import com.fabbitinc.server.domain.mapping.model.MappingRevision;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MappingRevisionRepository extends JpaRepository<MappingRevision, UUID> {

    Optional<MappingRevision> findFirstByRecordIdOrderByVersionDesc(UUID recordId);
}
