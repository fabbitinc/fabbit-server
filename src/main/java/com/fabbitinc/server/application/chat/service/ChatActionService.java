package com.fabbitinc.server.application.chat.service;

import com.fabbitinc.server.application.chat.support.ChatMessageComposer;
import com.fabbitinc.server.application.chat.support.ChatVisibleTraceFormatter;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.issue.usecase.CreateIssueUseCase;
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

    private final ChatService chatService;
    private final CreateIssueUseCase createIssueUseCase;
    private final ChatMessageComposer chatMessageComposer;
    private final ChatVisibleTraceFormatter chatVisibleTraceFormatter;
    private final ObjectMapper objectMapper;

    public ChatActionRequest createIssueDraft(ChatRun run, UUID partId, String title, String bodySummary) {
        ObjectNode preview = objectMapper.createObjectNode();
        preview.put("title", normalizeTitle(title));
        preview.putArray("partIds").add(partId.toString());
        preview.put("bodySummary", normalizeBodyText(bodySummary));

        ObjectNode request = objectMapper.createObjectNode();
        request.put("title", normalizeTitle(title));
        request.put("bodyText", normalizeBodyText(bodySummary));
        ArrayNode partIds = request.putArray("partIds");
        partIds.add(partId.toString());

        return chatService.createActionRequest(
                run,
                ChatActionRequestType.CREATE_ISSUE,
                write(preview),
                write(request),
                Instant.now().plus(30, ChronoUnit.MINUTES)
        );
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
            throw new AppException(ErrorCode.BAD_REQUEST, "지원하지 않는 챗 액션 타입입니다");
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
        chatService.publishEvent(run.getId(), "trace.updated", chatVisibleTraceFormatter.format(
                "이슈 생성 요청을 실행했습니다",
                "issue_create_confirm",
                "COMPLETED"
        ));
        chatService.publishEvent(run.getId(), "action.completed", Map.of(
                "actionRequestId", actionRequest.getId().toString(),
                "resourceType", "ISSUE",
                "resourceId", result.issueId().toString()
        ), ChatRunEventVisibility.USER_VISIBLE);
        chatService.appendAssistantNotice(
                actionRequest.getThreadId(),
                chatMessageComposer.actionExecutionResult("이슈를 생성했습니다.", result.issueId(), "ISSUE")
        );
        return new ConfirmedActionResult(actionRequest.getId(), actionRequest.getStatus(), result.issueId());
    }

    public void reject(UUID actionRequestId, UUID orgId, UUID userId) {
        ChatActionRequest actionRequest = chatService.getActionRequestOrThrow(actionRequestId);
        ChatRun run = chatService.getRunOrThrow(actionRequest.getRunId());
        chatService.getThreadOrThrow(actionRequest.getThreadId(), orgId, userId);
        actionRequest.reject();
        chatService.publishEvent(run.getId(), "action.rejected", Map.of(
                "actionRequestId", actionRequest.getId().toString(),
                "status", actionRequest.getStatus().name()
        ), ChatRunEventVisibility.USER_VISIBLE);
        chatService.appendAssistantNotice(
                actionRequest.getThreadId(),
                chatMessageComposer.assistantText("초안 실행을 취소했습니다.")
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
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "액션 요청 payload를 읽을 수 없습니다");
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
                .put("text", bodyText == null || bodyText.isBlank() ? "챗에서 생성한 이슈입니다." : bodyText);
        return root;
    }

    private String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return "챗 이슈 초안";
        }
        return title.trim();
    }

    private String normalizeBodyText(String bodyText) {
        if (bodyText == null || bodyText.isBlank()) {
            return "챗에서 생성한 이슈입니다.";
        }
        String trimmed = bodyText.trim();
        return trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed;
    }

    private String write(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JacksonException ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "액션 payload 직렬화에 실패했습니다");
        }
    }

    private UUID uuidOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return UUID.fromString(raw);
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
