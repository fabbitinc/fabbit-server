package com.fabbitinc.server.domain.synthesisv2.repository;

import com.fabbitinc.server.domain.synthesisv2.model.SynthesisV2Job;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SynthesisV2JobRepository extends JpaRepository<SynthesisV2Job, UUID> {

    List<SynthesisV2Job> findByBatchIdOrderByCreatedAtAsc(UUID batchId);

    List<SynthesisV2Job> findAllByOrderByCreatedAtDesc();
}
