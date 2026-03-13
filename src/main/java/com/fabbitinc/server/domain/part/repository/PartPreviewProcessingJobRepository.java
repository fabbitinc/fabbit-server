package com.fabbitinc.server.domain.part.repository;

import com.fabbitinc.server.domain.part.model.PartPreviewProcessingJob;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface PartPreviewProcessingJobRepository extends JpaRepository<PartPreviewProcessingJob, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select job from PartPreviewProcessingJob job where job.id = ?1")
    Optional<PartPreviewProcessingJob> findByIdForUpdate(UUID id);

    Optional<PartPreviewProcessingJob> findFirstByPartPreviewIdOrderByCreatedAtDesc(UUID partPreviewId);
}
