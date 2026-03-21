package com.fabbitinc.server.domain.chat.repository;

import com.fabbitinc.server.domain.chat.model.ChatRun;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRunRepository extends JpaRepository<ChatRun, UUID> {

    List<ChatRun> findByThreadIdOrderByCreatedAtDesc(UUID threadId);

    Optional<ChatRun> findByIdAndThreadId(UUID id, UUID threadId);
}
