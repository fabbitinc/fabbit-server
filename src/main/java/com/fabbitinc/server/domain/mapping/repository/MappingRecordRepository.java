package com.fabbitinc.server.domain.mapping.repository;

import com.fabbitinc.server.domain.mapping.model.MappingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MappingRecordRepository extends JpaRepository<MappingRecord, UUID> {

    Optional<MappingRecord> findByIdAndActiveTrue(UUID id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    List<MappingRecord> findByActiveTrueOrderByCreatedAtDesc();
}
