package com.fabbitinc.server.application.notification.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fabbitinc.server.application.notification.support.SseManager;
import com.fabbitinc.server.application.notification.usecase.command.PushNotificationStreamCommand;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class PushNotificationStreamUseCaseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void notification_created_sse_이벤트를_큐에_푸시한다() throws Exception {
        SseManager sseManager = new SseManager();
        PushNotificationStreamUseCase useCase = new PushNotificationStreamUseCase(sseManager, objectMapper);

        UUID notificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BlockingQueue<String> queue = sseManager.connect(userId);

        useCase.execute(new PushNotificationStreamCommand(notificationId, userId));

        String event = queue.poll();
        assertTrue(event.startsWith("event: notification.created\ndata: "));
        assertTrue(event.endsWith("\n\n"));

        String json = event.substring("event: notification.created\ndata: ".length(), event.length() - 2);
        JsonNode node = objectMapper.readTree(json);
        assertEquals(notificationId.toString(), node.get("notificationId").asText());
        assertEquals(userId.toString(), node.get("userId").asText());
    }
}
