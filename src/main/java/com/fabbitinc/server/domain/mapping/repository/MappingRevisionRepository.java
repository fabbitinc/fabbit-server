package com.fabbitinc.server.domain.mapping.repository;

import com.fabbitinc.server.domain.mapping.model.MappingRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MappingRevisionRepository extends JpaRepository<MappingRevision, UUID> {

    Optional<MappingRevision> findFirstByRecordIdOrderByVersionDesc(UUID recordId);
}
