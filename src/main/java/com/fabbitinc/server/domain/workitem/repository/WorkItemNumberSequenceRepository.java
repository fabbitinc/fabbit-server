package com.fabbitinc.server.domain.workitem.repository;

import com.fabbitinc.server.domain.workitem.model.WorkItemNumberSequence;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

public interface WorkItemNumberSequenceRepository extends JpaRepository<WorkItemNumberSequence, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select sequence from WorkItemNumberSequence sequence where sequence.id = ?1")
    Optional<WorkItemNumberSequence> findByIdForUpdate(UUID id);
}
