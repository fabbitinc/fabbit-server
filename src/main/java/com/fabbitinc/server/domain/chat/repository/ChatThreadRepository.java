package com.fabbitinc.server.domain.chat.repository;

import com.fabbitinc.server.domain.chat.model.ChatThread;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatThreadRepository extends JpaRepository<ChatThread, UUID> {

    List<ChatThread> findByOrgIdAndUserIdOrderByLastMessageAtDesc(UUID orgId, UUID userId);

    Optional<ChatThread> findByIdAndOrgIdAndUserId(UUID id, UUID orgId, UUID userId);
}
