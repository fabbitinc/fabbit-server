package com.fabbitinc.server.domain.chat.repository;

import com.fabbitinc.server.domain.chat.model.ChatThread;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

public interface ChatThreadRepository extends JpaRepository<ChatThread, UUID> {

    List<ChatThread> findByOrgIdAndUserIdOrderByLastMessageAtDesc(UUID orgId, UUID userId);

    Optional<ChatThread> findByIdAndOrgIdAndUserId(UUID id, UUID orgId, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select thread from ChatThread thread where thread.id = ?1 and thread.orgId = ?2 and thread.userId = ?3")
    Optional<ChatThread> findByIdAndOrgIdAndUserIdForUpdate(UUID id, UUID orgId, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select thread from ChatThread thread where thread.id = ?1")
    Optional<ChatThread> findByIdForUpdate(UUID id);
}
