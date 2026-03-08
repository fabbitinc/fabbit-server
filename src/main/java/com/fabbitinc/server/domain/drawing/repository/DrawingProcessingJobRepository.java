package com.fabbitinc.server.domain.drawing.repository;

import com.fabbitinc.server.domain.drawing.model.DrawingProcessingJob;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface DrawingProcessingJobRepository extends JpaRepository<DrawingProcessingJob, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DrawingProcessingJob> findByIdForUpdate(UUID id);
}
