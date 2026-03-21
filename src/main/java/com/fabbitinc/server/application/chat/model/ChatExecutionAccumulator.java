package com.fabbitinc.server.application.chat.model;

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
        this.pendingAction = new ChatPendingAction(
                actionRequest,
                ChatUiArtifact.of("action_request", actionRequestPayload)
        );
        addUiArtifact(this.pendingAction.uiArtifact());
        addToolName("issue_create_draft");
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
