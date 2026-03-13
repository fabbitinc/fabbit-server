package com.fabbitinc.server.domain.part.repository;

import com.fabbitinc.server.domain.part.model.PartPreviewServingProjection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartPreviewServingProjectionRepository extends JpaRepository<PartPreviewServingProjection, UUID> {
}
