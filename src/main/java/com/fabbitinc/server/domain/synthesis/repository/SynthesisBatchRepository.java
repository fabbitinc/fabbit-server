package com.fabbitinc.server.domain.synthesis.repository;

import com.fabbitinc.server.domain.synthesis.model.SynthesisBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SynthesisBatchRepository extends JpaRepository<SynthesisBatch, UUID> {
}
