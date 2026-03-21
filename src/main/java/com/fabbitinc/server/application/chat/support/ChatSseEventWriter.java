package com.fabbitinc.server.application.chat.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class ChatSseEventWriter {

    private final ObjectMapper objectMapper;

    public String write(long sequence, String eventType, Object payload) {
        return "id: " + sequence + "\n" +
                "event: " + eventType + "\n" +
                "data: " + serialize(payload) + "\n\n";
    }

    public String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException ex) {
            throw new IllegalStateException("SSE 이벤트 직렬화에 실패했습니다", ex);
        }
    }
}
