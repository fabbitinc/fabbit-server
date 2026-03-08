package com.fabbitinc.server.domain.drawing.repository;

import com.fabbitinc.server.domain.drawing.model.DrawingServingProjection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DrawingServingProjectionRepository extends JpaRepository<DrawingServingProjection, UUID> {
}
