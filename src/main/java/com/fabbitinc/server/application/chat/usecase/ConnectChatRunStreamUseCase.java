package com.fabbitinc.server.application.chat.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.chat.service.ChatService;
import com.fabbitinc.server.application.chat.support.ChatSseEventWriter;
import com.fabbitinc.server.application.chat.support.ChatSseManager;
import com.fabbitinc.server.application.chat.usecase.result.ChatRunStreamResult;
import com.fabbitinc.server.domain.chat.model.ChatRun;
import com.fabbitinc.server.domain.chat.model.ChatRunEvent;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConnectChatRunStreamUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final ChatService chatService;
    private final ChatSseManager chatSseManager;
    private final ChatSseEventWriter chatSseEventWriter;
    private final ObjectMapper objectMapper;

    public ChatRunStreamResult execute(UUID runId) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        ChatRun run = chatService.getRunOrThrow(runId);
        chatService.getThreadOrThrow(run.getThreadId(), auth.orgId(), auth.userId());

        BlockingQueue<String> queue = chatSseManager.connect(runId);
        for (ChatRunEvent event : chatService.getRunEvents(runId)) {
            queue.offer(chatSseEventWriter.write(event.getEventType(), parsePayload(event.getPayload())));
        }
        return new ChatRunStreamResult(runId, queue);
    }

    public void disconnect(ChatRunStreamResult result) {
        chatSseManager.disconnect(result.runId(), result.queue());
    }

    private Object parsePayload(String raw) {
        try {
            return objectMapper.readTree(raw);
        } catch (JacksonException ex) {
            return objectMapper.createObjectNode();
        }
    }
}
