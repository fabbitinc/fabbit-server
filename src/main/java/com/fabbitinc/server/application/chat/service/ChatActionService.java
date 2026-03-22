package com.fabbitinc.server.application.chat.service;

import com.fabbitinc.server.application.chat.support.ChatMessageComposer;
import com.fabbitinc.server.application.chat.support.ChatMessageCatalog;
import com.fabbitinc.server.application.chat.support.ChatEventTypes;
import com.fabbitinc.server.application.chat.support.ChatVisibleTraceFormatter;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.issue.usecase.CreateIssueUseCase;
import com.fabbitinc.server.application.part.api.PartSnapshot;
import com.fabbitinc.server.domain.chat.model.ChatActionRequest;
import com.fabbitinc.server.domain.chat.model.ChatActionRequestStatus;
import com.fabbitinc.server.domain.chat.model.ChatActionRequestType;
import com.fabbitinc.server.domain.chat.model.ChatRun;
import com.fabbitinc.server.domain.chat.model.ChatRunEventVisibility;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Service
@RequiredArgsConstructor
public class ChatActionService {

    private static final String ISSUE_CREATE_CONFIRM_TRACE_STEP = "issue_create_confirm";

    private final ChatService chatService;
    private final CreateIssueUseCase createIssueUseCase;
    private final ChatMessageComposer chatMessageComposer;
    private final ChatMessageCatalog chatMessageCatalog;
    private final ChatVisibleTraceFormatter chatVisibleTraceFormatter;
    private final ObjectMapper objectMapper;

    public ChatActionRequest createIssueDraft(ChatRun run, PartSnapshot part, String title, String bodySummary) {
        String normalizedTitle = normalizeTitle(title);
        String normalizedBodyText = normalizeBodyText(bodySummary);

        ObjectNode preview = createIssueDraftPreview(part, normalizedTitle, normalizedBodyText);
        ObjectNode request = createIssueDraftRequest(part.id(), normalizedTitle, normalizedBodyText);
        String requestPayload = write(request);

        return chatService.findLatestPendingActionRequest(run.getThreadId(), ChatActionRequestType.CREATE_ISSUE)
                .filter(existing -> existing.getRequestPayload().equals(requestPayload))
                .orElseGet(() -> chatService.createActionRequest(
                        run,
                        ChatActionRequestType.CREATE_ISSUE,
                        write(preview),
                        requestPayload,
                        Instant.now().plus(30, ChronoUnit.MINUTES)
                ));
    }

    public ConfirmedActionResult confirm(UUID actionRequestId, UUID orgId, UUID userId) {
        ChatActionRequest actionRequest = chatService.getActionRequestForUpdateOrThrow(actionRequestId);
        ChatRun run = chatService.getRunOrThrow(actionRequest.getRunId());
        chatService.getThreadOrThrow(actionRequest.getThreadId(), orgId, userId);

        if (actionRequest.getStatus() == ChatActionRequestStatus.EXECUTED) {
            JsonNode resultPayload = chatMessageComposer.parse(actionRequest.getResultPayload());
            UUID issueId = uuidOrNull(resultPayload.path("issueId").asText(null));
            return new ConfirmedActionResult(actionRequest.getId(), actionRequest.getStatus(), issueId);
        }

        actionRequest.confirm(userId);
        if (actionRequest.getActionType() != ChatActionRequestType.CREATE_ISSUE) {
            throw new AppException(ErrorCode.BAD_REQUEST, chatMessageCatalog.unsupportedChatActionType());
        }

        CreateIssueDraftRequest request = parseRequest(actionRequest.getRequestPayload());
        CreateIssueUseCase.CreateIssueResult result = createIssueUseCase.execute(
                new CreateIssueUseCase.CreateIssueCommand(
                        request.title(),
                        toTipTapDocument(request.bodyText()),
                        request.partIds(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                )
        );

        String resultPayload = chatMessageComposer.writeMap(Map.of("issueId", result.issueId().toString()));
        actionRequest.execute(resultPayload);
        chatService.publishEvent(run.getId(), ChatEventTypes.TRACE_UPDATED, chatVisibleTraceFormatter.format(
                chatMessageCatalog.issueCreateConfirmedTrace(),
                ISSUE_CREATE_CONFIRM_TRACE_STEP,
                "COMPLETED"
        ));
        chatService.publishEvent(run.getId(), ChatEventTypes.ACTION_COMPLETED, Map.of(
                "actionRequestId", actionRequest.getId().toString(),
                "resourceType", "ISSUE",
                "resourceId", result.issueId().toString()
        ), ChatRunEventVisibility.USER_VISIBLE);
        chatService.appendAssistantNotice(
                actionRequest.getThreadId(),
                chatMessageComposer.actionExecutionResult(chatMessageCatalog.issueCreated(), result.issueId(), "ISSUE")
        );
        return new ConfirmedActionResult(actionRequest.getId(), actionRequest.getStatus(), result.issueId());
    }

    public void reject(UUID actionRequestId, UUID orgId, UUID userId) {
        ChatActionRequest actionRequest = chatService.getActionRequestOrThrow(actionRequestId);
        ChatRun run = chatService.getRunOrThrow(actionRequest.getRunId());
        chatService.getThreadOrThrow(actionRequest.getThreadId(), orgId, userId);
        actionRequest.reject();
        chatService.publishEvent(run.getId(), ChatEventTypes.ACTION_REJECTED, Map.of(
                "actionRequestId", actionRequest.getId().toString(),
                "status", actionRequest.getStatus().name()
        ), ChatRunEventVisibility.USER_VISIBLE);
        chatService.appendAssistantNotice(
                actionRequest.getThreadId(),
                chatMessageComposer.assistantText(chatMessageCatalog.actionCancelled())
        );
    }

    private CreateIssueDraftRequest parseRequest(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            List<UUID> partIds = root.path("partIds").isArray()
                    ? java.util.stream.StreamSupport.stream(root.path("partIds").spliterator(), false)
                    .map(JsonNode::asText)
                    .map(UUID::fromString)
                    .toList()
                    : List.of();
            return new CreateIssueDraftRequest(
                    root.path("title").asText(),
                    root.path("bodyText").asText(),
                    partIds
            );
        } catch (JacksonException ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, chatMessageCatalog.actionRequestPayloadReadFailed());
        }
    }

    private JsonNode toTipTapDocument(String bodyText) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "doc");
        ArrayNode content = root.putArray("content");
        ObjectNode paragraph = content.addObject();
        paragraph.put("type", "paragraph");
        ArrayNode paragraphContent = paragraph.putArray("content");
        paragraphContent.addObject()
                .put("type", "text")
                .put("text", bodyText == null || bodyText.isBlank() ? chatMessageCatalog.issueDraftDefaultBody() : bodyText);
        return root;
    }

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return chatMessageCatalog.issueDraftDefaultTitle();
        }
        return title.trim();
    }

    private String normalizeBodyText(String bodyText) {
        if (bodyText == null || bodyText.isBlank()) {
            return chatMessageCatalog.issueDraftDefaultBody();
        }
        String trimmed = bodyText.trim();
        return trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed;
    }

    private String write(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JacksonException ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, chatMessageCatalog.actionPayloadSerializationFailed());
        }
    }

    private UUID uuidOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return UUID.fromString(raw);
    }

    public ObjectNode toActionRequestPayload(ChatActionRequest actionRequest) {
        JsonNode preview = chatMessageComposer.parse(actionRequest.getPreviewPayload());
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("actionRequestId", actionRequest.getId().toString());
        payload.put("actionType", actionRequest.getActionType().name());
        payload.put("status", actionRequest.getStatus().name());
        payload.set("preview", preview);
        return payload;
    }

    private ObjectNode createIssueDraftPreview(PartSnapshot part, String title, String bodySummary) {
        ObjectNode preview = objectMapper.createObjectNode();
        preview.put("title", title);
        preview.put("bodySummary", bodySummary);

        ObjectNode partNode = preview.putObject("part");
        partNode.put("number", normalizeDisplayValue(part.partNumber(), chatMessageCatalog.unknownPartDisplayName()));
        partNode.put("name", normalizeDisplayValue(part.name(), ""));
        return preview;
    }

    private ObjectNode createIssueDraftRequest(UUID partId, String title, String bodyText) {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("title", title);
        request.put("bodyText", bodyText);
        ArrayNode partIds = request.putArray("partIds");
        partIds.add(partId.toString());
        return request;
    }

    private String normalizeDisplayValue(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    public record ConfirmedActionResult(
            UUID actionRequestId,
            ChatActionRequestStatus status,
            UUID issueId
    ) {
    }

    private record CreateIssueDraftRequest(
            String title,
            String bodyText,
            List<UUID> partIds
    ) {
    }
}
