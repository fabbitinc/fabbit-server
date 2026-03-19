package com.fabbitinc.server.domain.mapping.repository;

import com.fabbitinc.server.domain.mapping.model.MappingRecord;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MappingRecordRepository extends JpaRepository<MappingRecord, UUID> {

    java.util.Optional<MappingRecord> findByIdAndActiveTrue(UUID id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    java.util.List<MappingRecord> findByActiveTrueOrderByCreatedAtDesc();
}
