package com.fabbitinc.server.domain.drawing.repository;

import com.fabbitinc.server.domain.drawing.model.DrawingProcessingJob;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

public interface DrawingProcessingJobRepository extends JpaRepository<DrawingProcessingJob, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select job from DrawingProcessingJob job where job.id = ?1")
    Optional<DrawingProcessingJob> findByIdForUpdate(UUID id);

    Optional<DrawingProcessingJob> findFirstByDrawingIdOrderByCreatedAtDesc(UUID drawingId);
}
