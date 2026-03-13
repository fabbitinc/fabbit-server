package com.fabbitinc.server.domain.drawing.repository;

import com.fabbitinc.server.domain.drawing.model.Drawing;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DrawingRepository extends JpaRepository<Drawing, UUID> {

    Optional<Drawing> findByDrawingNumberAndDeletedAtIsNull(String drawingNumber);

    List<Drawing> findByPartIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID partId);

    long countByPartIdAndDeletedAtIsNull(UUID partId);

    boolean existsByPartIdAndDeletedAtIsNull(UUID partId);

    long countByPartIdIsNotNullAndDeletedAtIsNull();

    List<Drawing> findByDrawingNumberContainingIgnoreCaseOrNameContainingIgnoreCaseOrderByDrawingNumberAsc(
            String drawingNumber,
            String name,
            Pageable pageable
    );
}
