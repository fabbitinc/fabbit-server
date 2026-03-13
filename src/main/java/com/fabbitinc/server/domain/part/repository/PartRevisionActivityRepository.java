package com.fabbitinc.server.domain.part.repository;

import com.fabbitinc.server.domain.part.model.PartRevisionActivity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartRevisionActivityRepository extends JpaRepository<PartRevisionActivity, UUID> {

    List<PartRevisionActivity> findByPartRevisionIdOrderByCreatedAtDesc(UUID partRevisionId);

    List<PartRevisionActivity> findByPartRevisionIdOrderByCreatedAtAsc(UUID partRevisionId);
}
