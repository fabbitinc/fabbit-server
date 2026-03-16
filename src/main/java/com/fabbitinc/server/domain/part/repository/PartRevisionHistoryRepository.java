package com.fabbitinc.server.domain.part.repository;

import com.fabbitinc.server.domain.part.model.PartRevisionHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartRevisionHistoryRepository extends JpaRepository<PartRevisionHistory, UUID> {

    List<PartRevisionHistory> findByPartRevisionIdOrderByCreatedAtDesc(UUID partRevisionId);

    List<PartRevisionHistory> findByPartRevisionIdOrderByCreatedAtAsc(UUID partRevisionId);

    List<PartRevisionHistory> findByPartRevisionIdInOrderByOccurredAtAsc(List<UUID> partRevisionIds);
}
