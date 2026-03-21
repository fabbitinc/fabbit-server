package com.fabbitinc.server.domain.chat.repository;

import com.fabbitinc.server.domain.chat.model.ChatActionRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatActionRequestRepository extends JpaRepository<ChatActionRequest, UUID> {

    List<ChatActionRequest> findByThreadIdOrderByCreatedAtDesc(UUID threadId);

    Optional<ChatActionRequest> findTopByRunIdOrderByCreatedAtDesc(UUID runId);
}
