package com.fabbitinc.server.application.chat.support;

import com.fabbitinc.server.application.issue.api.IssueSnapshot;
import com.fabbitinc.server.application.part.api.PartSnapshot;
import com.fabbitinc.server.domain.chat.model.ChatActionRequest;
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
        ObjectNode root = objectMapper.createObjectNode();
        root.put("text", normalizeText(text));
        ArrayNode blocks = root.putArray("blocks");
        blocks.addObject()
                .put("type", "text")
                .put("text", normalizeText(text));
        return write(root);
    }

    public String partLookupResult(String text, List<PartSnapshot> partSnapshots, List<IssueSnapshot> issueSnapshots) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("text", normalizeText(text));
        ArrayNode blocks = root.putArray("blocks");
        blocks.addObject()
                .put("type", "text")
                .put("text", normalizeText(text));

        ArrayNode partItems = objectMapper.createArrayNode();
        for (PartSnapshot snapshot : partSnapshots) {
            partItems.addObject()
                    .put("id", stringValue(snapshot.id()))
                    .put("revisionId", stringValue(snapshot.revisionId()))
                    .put("partNumber", snapshot.partNumber())
                    .put("name", snapshot.name())
                    .put("revisionCode", snapshot.revisionCode());
        }
        blocks.addObject()
                .put("type", "part_lookup_result")
                .set("items", partItems);

        if (!issueSnapshots.isEmpty()) {
            ArrayNode issueItems = objectMapper.createArrayNode();
            for (IssueSnapshot snapshot : issueSnapshots) {
                issueItems.addObject()
                        .put("id", stringValue(snapshot.id()))
                        .put("number", snapshot.number())
                        .put("title", snapshot.title())
                        .put("state", snapshot.state().name());
            }
            blocks.addObject()
                    .put("type", "issue_lookup_result")
                    .set("items", issueItems);
        }

        return write(root);
    }

    public String issueDraftResult(String text, ChatActionRequest actionRequest, JsonNode previewPayload) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("text", normalizeText(text));
        ArrayNode blocks = root.putArray("blocks");
        blocks.addObject()
                .put("type", "text")
                .put("text", normalizeText(text));
        blocks.addObject()
                .put("type", "action_request")
                .put("actionRequestId", actionRequest.getId().toString())
                .put("actionType", actionRequest.getActionType().name())
                .set("preview", previewPayload);
        return write(root);
    }

    public String actionExecutionResult(String text, UUID resourceId, String resourceType) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("text", normalizeText(text));
        ArrayNode blocks = root.putArray("blocks");
        blocks.addObject()
                .put("type", "text")
                .put("text", normalizeText(text));
        blocks.addObject()
                .put("type", "action_result")
                .put("resourceType", resourceType)
                .put("resourceId", stringValue(resourceId));
        return write(root);
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

    public String writeMap(Map<String, Object> payload) {
        return write(objectMapper.valueToTree(payload));
    }

    private String normalizeText(String value) {
        return value == null ? "" : value;
    }

    private String stringValue(UUID value) {
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
