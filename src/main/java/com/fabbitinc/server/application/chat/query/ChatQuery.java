package com.fabbitinc.server.application.chat.query;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.chat.query.condition.ChatMessageListCondition;
import com.fabbitinc.server.application.chat.query.condition.ChatRunEventListCondition;
import com.fabbitinc.server.application.chat.query.condition.ChatThreadDetailCondition;
import com.fabbitinc.server.application.chat.query.condition.ChatThreadListCondition;
import com.fabbitinc.server.application.chat.query.result.ChatMessageListResult;
import com.fabbitinc.server.application.chat.query.result.ChatRunEventListResult;
import com.fabbitinc.server.application.chat.query.result.ChatThreadDetailResult;
import com.fabbitinc.server.application.chat.query.result.ChatThreadListResult;
import com.fabbitinc.server.application.chat.support.ChatArtifactTypes;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.domain.chat.model.ChatMessageRole;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.chat.model.ChatActionRequest;
import com.fabbitinc.server.domain.chat.model.ChatRun;
import com.fabbitinc.server.domain.chat.model.ChatThread;
import com.fabbitinc.server.domain.chat.repository.ChatActionRequestRepository;
import com.fabbitinc.server.domain.chat.repository.ChatMessageRepository;
import com.fabbitinc.server.domain.chat.repository.ChatRunEventRepository;
import com.fabbitinc.server.domain.chat.repository.ChatRunRepository;
import com.fabbitinc.server.domain.chat.repository.ChatThreadRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatQuery {

    private final CurrentAuthProvider currentAuthProvider;
    private final ChatThreadRepository chatThreadRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRunRepository chatRunRepository;
    private final ChatRunEventRepository chatRunEventRepository;
    private final ChatActionRequestRepository chatActionRequestRepository;
    private final ObjectMapper objectMapper;

    public ChatThreadListResult list(ChatThreadListCondition condition) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        List<ChatThreadListResult.Item> items = chatThreadRepository.findByOrgIdAndUserIdOrderByLastMessageAtDesc(
                        auth.orgId(),
                        auth.userId()
                ).stream()
                .map(thread -> new ChatThreadListResult.Item(
                        thread.getId(),
                        thread.getProjectId(),
                        thread.getContextType(),
                        thread.getContextId(),
                        thread.getTitle(),
                        thread.getStatus(),
                        thread.getLastMessageAt(),
                        thread.getCreatedAt()
                ))
                .toList();
        return new ChatThreadListResult(items);
    }

    public ChatThreadDetailResult get(ChatThreadDetailCondition condition) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        ChatThread thread = chatThreadRepository.findByIdAndOrgIdAndUserId(condition.threadId(), auth.orgId(), auth.userId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "챗 스레드를 찾을 수 없습니다"));
        return new ChatThreadDetailResult(
                thread.getId(),
                thread.getProjectId(),
                thread.getContextType(),
                thread.getContextId(),
                thread.getTitle(),
                thread.getStatus(),
                thread.getLastMessageAt(),
                thread.getCreatedAt()
        );
    }

    public ChatMessageListResult list(ChatMessageListCondition condition) {
        get(new ChatThreadDetailCondition(condition.threadId()));
        List<ChatMessageListResult.Item> items = chatMessageRepository.findByThreadIdOrderBySequenceAsc(condition.threadId()).stream()
                .filter(message -> message.getRole() != ChatMessageRole.SYSTEM)
                .map(message -> new ChatMessageListResult.Item(
                        message.getId(),
                        message.getRunId(),
                        message.getRole(),
                        message.getMessageType(),
                        message.getStatus(),
                        message.getSequence(),
                        hydrateMessageContent(message.getContent()),
                        message.getCreatedAt()
                ))
                .toList();
        return new ChatMessageListResult(items);
    }

    public ChatRunEventListResult list(ChatRunEventListCondition condition) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        ChatRun run = chatRunRepository.findById(condition.runId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "챗 실행 정보를 찾을 수 없습니다"));
        chatThreadRepository.findByIdAndOrgIdAndUserId(run.getThreadId(), auth.orgId(), auth.userId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "챗 스레드를 찾을 수 없습니다"));

        List<ChatRunEventListResult.Item> items = chatRunEventRepository.findByRunIdOrderBySequenceAsc(condition.runId()).stream()
                .map(event -> new ChatRunEventListResult.Item(
                        event.getId(),
                        event.getRunId(),
                        event.getSequence(),
                        event.getEventType(),
                        event.getVisibility(),
                        event.getPayload(),
                        event.getCreatedAt()
                ))
                .toList();
        return new ChatRunEventListResult(items);
    }

    private String hydrateMessageContent(String content) {
        try {
            JsonNode rootNode = objectMapper.readTree(content);
            if (!(rootNode instanceof ObjectNode root)) {
                return content;
            }

            JsonNode blocksNode = root.get("blocks");
            if (!(blocksNode instanceof ArrayNode blocks)) {
                return content;
            }

            boolean hasActionRequest = false;
            for (JsonNode blockNode : blocks) {
                if (!(blockNode instanceof ObjectNode block)) {
                    continue;
                }
                if (!ChatArtifactTypes.ACTION_REQUEST.equals(block.path("type").asText())) {
                    continue;
                }
                hasActionRequest = true;
                hydrateActionRequestBlock(block);
            }

            if (hasActionRequest) {
                removeRedundantPartListBlocks(blocks);
            }

            return objectMapper.writeValueAsString(root);
        } catch (JacksonException ex) {
            return content;
        }
    }

    private void hydrateActionRequestBlock(ObjectNode block) {
        JsonNode payloadNode = block.get("payload");
        if (!(payloadNode instanceof ObjectNode payload)) {
            return;
        }
        String actionRequestId = payload.path("actionRequestId").asText(null);
        if (actionRequestId == null || actionRequestId.isBlank()) {
            return;
        }

        findActionRequest(actionRequestId).ifPresent(actionRequest -> {
            payload.put("status", actionRequest.getStatus().name());
            JsonNode preview = parseJson(actionRequest.getPreviewPayload());
            if (preview != null) {
                payload.set("preview", preview);
            }
        });
    }

    private Optional<ChatActionRequest> findActionRequest(String actionRequestId) {
        try {
            return chatActionRequestRepository.findById(java.util.UUID.fromString(actionRequestId));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private JsonNode parseJson(String raw) {
        try {
            return objectMapper.readTree(raw);
        } catch (JacksonException ex) {
            return null;
        }
    }

    private void removeRedundantPartListBlocks(ArrayNode blocks) {
        for (int index = blocks.size() - 1; index >= 0; index--) {
            JsonNode blockNode = blocks.get(index);
            if (!(blockNode instanceof ObjectNode block)) {
                continue;
            }
            if (!ChatArtifactTypes.ENTITY_LIST.equals(block.path("type").asText())) {
                continue;
            }
            JsonNode payload = block.get("payload");
            if (payload == null || !"PART".equalsIgnoreCase(payload.path("entityType").asText())) {
                continue;
            }
            JsonNode items = payload.get("items");
            if (items != null && items.isArray() && items.size() == 1) {
                blocks.remove(index);
            }
        }
    }
}
