package com.fabbitinc.server.domain.mappingv2.repository;

import com.fabbitinc.server.domain.mappingv2.model.MappingV2Revision;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MappingV2RevisionRepository extends JpaRepository<MappingV2Revision, UUID> {

    Optional<MappingV2Revision> findFirstByRecordIdOrderByVersionDesc(UUID recordId);
}
