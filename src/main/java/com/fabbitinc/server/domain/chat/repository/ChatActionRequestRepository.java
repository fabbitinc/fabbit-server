package com.fabbitinc.server.domain.chat.repository;

import com.fabbitinc.server.domain.chat.model.ChatActionRequest;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

public interface ChatActionRequestRepository extends JpaRepository<ChatActionRequest, UUID> {

    List<ChatActionRequest> findByThreadIdOrderByCreatedAtDesc(UUID threadId);

    Optional<ChatActionRequest> findTopByRunIdOrderByCreatedAtDesc(UUID runId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select actionRequest from ChatActionRequest actionRequest where actionRequest.id = ?1")
    Optional<ChatActionRequest> findByIdForUpdate(UUID actionRequestId);
}
