package com.fabbitinc.server.domain.chat.repository;

import com.fabbitinc.server.domain.chat.model.ChatMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByThreadIdOrderBySequenceAsc(UUID threadId);

    List<ChatMessage> findByThreadIdAndSequenceLessThanEqualOrderBySequenceDesc(UUID threadId, long sequence, Pageable pageable);

    long countByThreadId(UUID threadId);
}
