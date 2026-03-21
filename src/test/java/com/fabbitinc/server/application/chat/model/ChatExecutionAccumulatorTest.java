package com.fabbitinc.server.application.chat.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fabbitinc.server.domain.chat.model.ChatActionRequest;
import com.fabbitinc.server.domain.chat.model.ChatActionRequestType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class ChatExecutionAccumulatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void recordPendingAction_adds_action_request_artifact() throws Exception {
        ChatExecutionAccumulator accumulator = new ChatExecutionAccumulator();
        ChatActionRequest actionRequest = ChatActionRequest.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                ChatActionRequestType.CREATE_ISSUE,
                "{\"title\":\"draft\"}",
                "{\"title\":\"draft\"}",
                Instant.now().plusSeconds(60)
        );

        accumulator.recordPendingAction(actionRequest, objectMapper.readTree("{\"title\":\"draft\"}"));

        assertNotNull(accumulator.getPendingAction());
        assertEquals(1, accumulator.getUiArtifacts().size());
        assertEquals("action_request", accumulator.getUiArtifacts().get(0).type());
        assertEquals("draft", accumulator.getUiArtifacts().get(0).payload().path("title").asText());
    }
}
