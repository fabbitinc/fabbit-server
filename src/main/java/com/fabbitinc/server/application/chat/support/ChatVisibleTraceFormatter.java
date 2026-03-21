package com.fabbitinc.server.application.chat.support;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ChatVisibleTraceFormatter {

    public Map<String, Object> format(String message, String step, String status) {
        return Map.of(
                "message", message,
                "step", step,
                "status", status
        );
    }
}
