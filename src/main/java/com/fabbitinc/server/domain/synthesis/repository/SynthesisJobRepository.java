package com.fabbitinc.server.domain.synthesis.repository;

import com.fabbitinc.server.domain.synthesis.model.SynthesisJob;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SynthesisJobRepository extends JpaRepository<SynthesisJob, UUID> {

    List<SynthesisJob> findByBatchIdOrderByCreatedAtAsc(UUID batchId);

    List<SynthesisJob> findAllByOrderByCreatedAtDesc();
}
