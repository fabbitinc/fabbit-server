package com.fabbitinc.server.domain.chat.repository;

import com.fabbitinc.server.domain.chat.model.ChatRunEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRunEventRepository extends JpaRepository<ChatRunEvent, UUID> {

    List<ChatRunEvent> findByRunIdOrderBySequenceAsc(UUID runId);

    List<ChatRunEvent> findByRunIdAndSequenceGreaterThanOrderBySequenceAsc(UUID runId, long sequence);

    long countByRunId(UUID runId);
}
