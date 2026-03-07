package com.fabbitinc.server.domain.synthesisv2.repository;

import com.fabbitinc.server.domain.synthesisv2.model.SynthesisV2Batch;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SynthesisV2BatchRepository extends JpaRepository<SynthesisV2Batch, UUID> {
}
