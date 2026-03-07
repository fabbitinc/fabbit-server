package com.fabbitinc.server.domain.mappingv2.repository;

import com.fabbitinc.server.domain.mappingv2.model.MappingV2Record;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MappingV2RecordRepository extends JpaRepository<MappingV2Record, UUID> {

    java.util.Optional<MappingV2Record> findByIdAndActiveTrue(UUID id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    java.util.List<MappingV2Record> findByActiveTrueOrderByCreatedAtDesc();
}
