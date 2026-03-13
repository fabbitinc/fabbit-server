package com.fabbitinc.server.domain.part.repository;

import com.fabbitinc.server.domain.part.model.Part;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartRepository extends JpaRepository<Part, UUID> {

    long countByCreatedAtGreaterThanEqual(Instant since);

    List<Part> findByPartNumberContainingIgnoreCaseOrderByPartNumberAsc(String partNumber, Pageable pageable);

    List<Part> findAllByOrderByPartNumberAsc(Pageable pageable);

    List<Part> findByIdInOrderByPartNumberAsc(Collection<UUID> ids);

    List<Part> findByPartNumberIn(Collection<String> partNumbers);

    Optional<Part> findByPartNumber(String partNumber);
}
