package com.fabbitinc.server.application.chat.model;

import com.fabbitinc.server.application.chat.support.ChatArtifactTypes;
import com.fabbitinc.server.domain.chat.model.ChatActionRequest;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import tools.jackson.databind.JsonNode;

public class ChatExecutionAccumulator {

    private final List<ChatUiArtifact> uiArtifacts = new ArrayList<>();
    private final Set<String> toolNames = new LinkedHashSet<>();
    private ChatPendingAction pendingAction;

    public void addUiArtifact(ChatUiArtifact uiArtifact) {
        if (uiArtifact == null) {
            return;
        }
        this.uiArtifacts.add(uiArtifact);
    }

    public void addToolName(String toolName) {
        if (toolName == null || toolName.isBlank()) {
            return;
        }
        this.toolNames.add(toolName.trim());
    }

    public void recordPendingAction(ChatActionRequest actionRequest, JsonNode actionRequestPayload) {
        if (actionRequest == null || actionRequestPayload == null) {
            return;
        }
        removeRedundantPartListArtifacts();
        this.pendingAction = new ChatPendingAction(
                actionRequest,
                ChatUiArtifact.of(ChatArtifactTypes.ACTION_REQUEST, actionRequestPayload)
        );
        addUiArtifact(this.pendingAction.uiArtifact());
    }

    private void removeRedundantPartListArtifacts() {
        uiArtifacts.removeIf(this::isSinglePartEntityListArtifact);
    }

    private boolean isSinglePartEntityListArtifact(ChatUiArtifact uiArtifact) {
        if (uiArtifact == null || !ChatArtifactTypes.ENTITY_LIST.equals(uiArtifact.type()) || uiArtifact.payload() == null) {
            return false;
        }
        JsonNode payload = uiArtifact.payload();
        JsonNode entityType = payload.get("entityType");
        JsonNode items = payload.get("items");
        return entityType != null
                && "PART".equalsIgnoreCase(entityType.asText())
                && items != null
                && items.isArray()
                && items.size() == 1;
    }

    public List<ChatUiArtifact> getUiArtifacts() {
        return List.copyOf(uiArtifacts);
    }

    public Set<String> getToolNames() {
        return Set.copyOf(toolNames);
    }

    public ChatPendingAction getPendingAction() {
        return pendingAction;
    }
}
