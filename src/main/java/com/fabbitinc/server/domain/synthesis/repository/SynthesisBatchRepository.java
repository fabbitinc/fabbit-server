package com.fabbitinc.server.domain.synthesis.repository;

import com.fabbitinc.server.domain.synthesis.model.SynthesisBatch;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SynthesisBatchRepository extends JpaRepository<SynthesisBatch, UUID> {
}
