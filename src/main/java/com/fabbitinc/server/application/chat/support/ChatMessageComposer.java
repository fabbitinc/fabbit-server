package com.fabbitinc.server.application.chat.support;

import com.fabbitinc.server.application.chat.model.ChatUiArtifact;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Component
@RequiredArgsConstructor
public class ChatMessageComposer {

    private final ObjectMapper objectMapper;

    public String userText(String text) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("text", normalizeText(text));
        return write(root);
    }

    public String assistantText(String text) {
        return assistantStructured(text, List.of());
    }

    public String assistantStructured(String text, List<ChatUiArtifact> uiArtifacts) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("text", normalizeText(text));

        ArrayNode blocks = root.putArray("blocks");
        blocks.addObject()
                .put("type", "text")
                .put("text", normalizeText(text));

        for (ChatUiArtifact uiArtifact : uiArtifacts) {
            if (uiArtifact == null) {
                continue;
            }
            blocks.addObject()
                    .put("type", uiArtifact.type())
                    .set("payload", uiArtifact.payload());
        }
        return write(root);
    }

    public String actionExecutionResult(String text, UUID resourceId, String resourceType) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("resourceType", resourceType);
        payload.put("resourceId", stringValue(resourceId));
        return assistantStructured(text, List.of(ChatUiArtifact.of("action_result", payload)));
    }

    public String errorText(String text) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("text", normalizeText(text));
        root.put("error", true);
        return write(root);
    }

    public String emptyAssistantContent() {
        return assistantText("");
    }

    public String extractText(String content) {
        try {
            return objectMapper.readTree(content).path("text").asText("");
        } catch (JacksonException ex) {
            return "";
        }
    }

    public JsonNode parse(String raw) {
        try {
            return objectMapper.readTree(raw);
        } catch (JacksonException ex) {
            throw new IllegalStateException("챗 메시지 JSON을 읽을 수 없습니다", ex);
        }
    }

    public JsonNode toJsonNode(Object payload) {
        return objectMapper.valueToTree(payload);
    }

    public String writeMap(Map<String, Object> payload) {
        return write(objectMapper.valueToTree(payload));
    }

    public String normalizeText(String value) {
        return value == null ? "" : value;
    }

    public String stringValue(UUID value) {
        return value == null ? null : value.toString();
    }

    private String write(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JacksonException ex) {
            throw new IllegalStateException("챗 메시지 JSON 직렬화에 실패했습니다", ex);
        }
    }
}
