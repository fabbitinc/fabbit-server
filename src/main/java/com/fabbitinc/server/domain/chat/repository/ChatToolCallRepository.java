package com.fabbitinc.server.domain.chat.repository;

import com.fabbitinc.server.domain.chat.model.ChatToolCall;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatToolCallRepository extends JpaRepository<ChatToolCall, UUID> {

    List<ChatToolCall> findByRunIdOrderByCreatedAtAsc(UUID runId);
}
