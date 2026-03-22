package com.fabbitinc.server.application.chat.tool;

import com.fabbitinc.server.application.chat.service.ChatActionService;
import com.fabbitinc.server.application.chat.service.ChatService;
import com.fabbitinc.server.application.chat.service.ChatToolExecutionService;
import com.fabbitinc.server.application.chat.service.ChatToolExecutionService.ToolProgressPayload;
import com.fabbitinc.server.application.chat.support.ChatEventTypes;
import com.fabbitinc.server.application.chat.support.ChatMessageCatalog;
import com.fabbitinc.server.application.part.api.PartApi;
import com.fabbitinc.server.application.part.api.PartSnapshot;
import com.fabbitinc.server.domain.chat.model.ChatActionRequest;
import com.fabbitinc.server.domain.chat.model.ChatActionRequestType;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class IssueCreateDraftTool {

    private static final String TOOL_NAME = "issue_create_draft";
    private static final String TOOL_DISPLAY_NAME = "이슈 초안 생성";
    private static final String TRACE_STEP = "issue_create_draft";

    private final PartApi partApi;
    private final ChatService chatService;
    private final ChatActionService chatActionService;
    private final ChatToolExecutionService chatToolExecutionService;
    private final ChatMessageCatalog chatMessageCatalog;

    public IssueCreateDraftTool(
            PartApi partApi,
            ChatService chatService,
            ChatActionService chatActionService,
            ChatToolExecutionService chatToolExecutionService,
            ChatMessageCatalog chatMessageCatalog
    ) {
        this.partApi = partApi;
        this.chatService = chatService;
        this.chatActionService = chatActionService;
        this.chatToolExecutionService = chatToolExecutionService;
        this.chatMessageCatalog = chatMessageCatalog;
    }

    @Tool(
            name = TOOL_NAME,
            description = """
                    새 이슈를 등록하기 위한 초안을 만들 때 사용합니다.
                    실제 이슈를 생성하지 않고, 사용자 확인이 필요한 draft만 준비합니다.
                    기존 이슈 수정, 상태 변경, 담당자 변경, 기한 추가 같은 업데이트 작업에는 사용하지 않습니다.
                    본문은 업무형 초안처럼 반정형으로 정리하고, 가능하면 현상, 영향 또는 배경, 추가 확인 필요 정보, 요청사항, 기한 항목이 드러나게 작성합니다.
                    정보가 없는 항목은 미확인, 미제공, 추가 확인 필요처럼 표시합니다.
                    """
    )
    public IssueCreateDraftToolResult issueCreateDraft(
            @ToolParam(description = "이슈를 연결할 부품 ID") String partId,
            @ToolParam(required = false, description = "제안할 이슈 제목") String title,
            @ToolParam(required = false, description = "제안할 이슈 본문 요약") String bodySummary,
            ToolContext toolContext
    ) {
        UUID resolvedPartId = UUID.fromString(partId);
        var runId = ChatToolContextSupport.getRunId(toolContext);
        var run = chatService.getRunOrThrow(runId);

        IssueCreateDraftToolResult result = chatToolExecutionService.execute(
                runId,
                run.getThreadId(),
                TOOL_NAME,
                TOOL_DISPLAY_NAME,
                Map.of("partId", resolvedPartId.toString()),
                () -> createDraft(runId, resolvedPartId, title, bodySummary, toolContext),
                executionResult -> executionResult,
                draftResult -> draftResult.requiresConfirmation()
                        ? chatMessageCatalog.issueDraftWaitingSummary()
                        : chatMessageCatalog.issueDraftTargetPartNotFound(),
                chatMessageCatalog.issueDraftStarted(),
                draftResult -> ToolProgressPayload.empty(
                        draftResult.requiresConfirmation()
                                ? chatMessageCatalog.issueDraftCreated()
                                : chatMessageCatalog.issueDraftTargetPartNotFound()
                ),
                chatMessageCatalog.issueDraftTraceCompleted(),
                TRACE_STEP
        );
        ChatToolContextSupport.getAccumulator(toolContext).addToolName(TOOL_NAME);

        if (result.requiresConfirmation() && result.preview() != null) {
            chatService.publishEvent(runId, ChatEventTypes.ACTION_REQUIRED, Map.of(
                    "actionRequestId", result.actionRequestId(),
                    "actionType", ChatActionRequestType.CREATE_ISSUE.name(),
                    "status", "PENDING",
                    "preview", result.preview()
            ));
        }

        return result;
    }

    private String resolveTitle(String title, String partNumber) {
        if (title == null || title.isBlank()) {
            return partNumber + " 관련 이슈";
        }
        return title.trim();
    }

    private String resolveBodySummary(String bodySummary, String question) {
        String baseText = bodySummary == null || bodySummary.isBlank() ? question : bodySummary;
        if (baseText == null || baseText.isBlank()) {
            return chatMessageCatalog.issueDraftDefaultBody();
        }
        return baseText.length() > 200 ? baseText.substring(0, 200) : baseText.trim();
    }

    private IssueCreateDraftToolResult createDraft(
            UUID runId,
            UUID partId,
            String title,
            String bodySummary,
            ToolContext toolContext
    ) {
        PartSnapshot part = partApi.getPartSnapshotMap(Set.of(partId)).get(partId);
        if (part == null) {
            return new IssueCreateDraftToolResult(chatMessageCatalog.issueDraftTargetPartMissingResponse(), false, null, null);
        }

        ChatActionRequest actionRequest = chatActionService.createIssueDraft(
                chatService.getRunOrThrow(runId),
                part,
                resolveTitle(title, part.partNumber()),
                resolveBodySummary(bodySummary, ChatToolContextSupport.getQuestion(toolContext))
        );
        var actionRequestPayload = chatActionService.toActionRequestPayload(actionRequest);
        ChatToolContextSupport.getAccumulator(toolContext).recordPendingAction(actionRequest, actionRequestPayload);
        return new IssueCreateDraftToolResult(
                chatMessageCatalog.issueDraftCreatedResponse(),
                true,
                actionRequest.getId().toString(),
                actionRequestPayload.path("preview")
        );
    }

    public record IssueCreateDraftToolResult(
            String summary,
            boolean requiresConfirmation,
            String actionRequestId,
            Object preview
    ) {
    }
}
