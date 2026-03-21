package com.fabbitinc.server.domain.chat.repository;

import com.fabbitinc.server.domain.chat.model.ChatRun;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

public interface ChatRunRepository extends JpaRepository<ChatRun, UUID> {

    List<ChatRun> findByThreadIdOrderByCreatedAtDesc(UUID threadId);

    Optional<ChatRun> findByIdAndThreadId(UUID id, UUID threadId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select run from ChatRun run where run.id = ?1")
    Optional<ChatRun> findByIdForUpdate(UUID id);
}
