package com.fabbitinc.server.domain.part.repository;

import com.fabbitinc.server.domain.part.model.PartPreview;
import com.fabbitinc.server.domain.part.model.PartPreviewSourceType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartPreviewRepository extends JpaRepository<PartPreview, UUID> {

    Optional<PartPreview> findByPartRevisionId(UUID partRevisionId);

    List<PartPreview> findBySourceTypeAndSourceId(PartPreviewSourceType sourceType, UUID sourceId);
}
