package com.fabbitinc.server.application.chat.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fabbitinc.server.application.chat.model.ChatUiArtifact;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

class ChatMessageComposerTest {

    private final ChatMessageComposer chatMessageComposer = new ChatMessageComposer(new ObjectMapper());

    @Test
    void assistantStructured_includes_text_and_artifact_blocks() {
        ObjectNode payload = JsonNodeFactory.instance.objectNode();
        payload.put("title", "부품 후보");

        String content = chatMessageComposer.assistantStructured(
                "조회 결과입니다.",
                List.of(ChatUiArtifact.of("entity_list", payload))
        );

        var root = chatMessageComposer.parse(content);
        assertEquals("조회 결과입니다.", root.path("text").asText());
        assertEquals("text", root.path("blocks").get(0).path("type").asText());
        assertEquals("entity_list", root.path("blocks").get(1).path("type").asText());
        assertEquals("부품 후보", root.path("blocks").get(1).path("payload").path("title").asText());
    }
}
