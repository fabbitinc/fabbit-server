package com.fabbitinc.server.domain.chat.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fabbitinc.server.domain.common.exception.DomainException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChatToolCallTest {

    @Test
    void create_initializes_started_state() {
        ChatToolCall toolCall = ChatToolCall.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "part_lookup",
                "{\"keyword\":\"A-1000\"}"
        );

        assertEquals(ChatToolCallStatus.STARTED, toolCall.getStatus());
        assertEquals("part_lookup", toolCall.getToolName());
        assertEquals("{\"keyword\":\"A-1000\"}", toolCall.getArgumentsJson());
        assertNotNull(toolCall.getStartedAt());
    }

    @Test
    void complete_transitions_to_completed() {
        ChatToolCall toolCall = ChatToolCall.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "part_lookup",
                "{}"
        );

        toolCall.complete("{\"items\":[]}");

        assertEquals(ChatToolCallStatus.COMPLETED, toolCall.getStatus());
        assertEquals("{\"items\":[]}", toolCall.getResultJson());
        assertNotNull(toolCall.getCompletedAt());
    }

    @Test
    void fail_transitions_to_failed() {
        ChatToolCall toolCall = ChatToolCall.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "part_lookup",
                "{}"
        );

        toolCall.fail("TIMEOUT", "{\"message\":\"timeout\"}");

        assertEquals(ChatToolCallStatus.FAILED, toolCall.getStatus());
        assertEquals("TIMEOUT", toolCall.getErrorCode());
        assertEquals("{\"message\":\"timeout\"}", toolCall.getResultJson());
        assertNotNull(toolCall.getCompletedAt());
    }

    @Test
    void complete_after_terminal_state_throws() {
        ChatToolCall toolCall = ChatToolCall.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "part_lookup",
                "{}"
        );
        toolCall.complete("{\"items\":[]}");

        DomainException ex = assertThrows(DomainException.class, () -> toolCall.complete("{}"));

        assertEquals(ChatToolCall.CODE_CHAT_TOOL_CALL_INVALID_STATE, ex.getDomainCode());
    }
}
