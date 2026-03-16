package com.fabbitinc.server.domain.issue.repository;

import com.fabbitinc.server.domain.issue.model.EngineeringChangeComment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EngineeringChangeCommentRepository extends JpaRepository<EngineeringChangeComment, UUID> {

    List<EngineeringChangeComment> findByEngineeringChangeIdOrderByCreatedAtAsc(UUID engineeringChangeId);
}
