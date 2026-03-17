package com.fabbitinc.server.domain.part.repository;

import com.fabbitinc.server.domain.part.model.PartPreviewFile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartPreviewFileRepository extends JpaRepository<PartPreviewFile, UUID> {

    Optional<PartPreviewFile> findByIdAndPartPreview_Id(UUID id, UUID partPreviewId);

    List<PartPreviewFile> findByPartPreview_IdOrderByCreatedAtDesc(UUID partPreviewId);
}
