package com.fabbitinc.server.application.chat.tool;

import com.fabbitinc.server.application.chat.service.ChatActionService;
import com.fabbitinc.server.application.chat.service.ChatService;
import com.fabbitinc.server.application.chat.service.ChatToolExecutionService;
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

    private final PartApi partApi;
    private final ChatService chatService;
    private final ChatActionService chatActionService;
    private final ChatToolExecutionService chatToolExecutionService;

    public IssueCreateDraftTool(
            PartApi partApi,
            ChatService chatService,
            ChatActionService chatActionService,
            ChatToolExecutionService chatToolExecutionService
    ) {
        this.partApi = partApi;
        this.chatService = chatService;
        this.chatActionService = chatActionService;
        this.chatToolExecutionService = chatToolExecutionService;
    }

    @Tool(
            name = "issue_create_draft",
            description = "이슈 생성 초안을 만듭니다. 실제 이슈를 생성하지 않고 사용자 확인이 필요한 draft만 생성합니다."
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
                "issue_create_draft",
                "이슈 초안 생성",
                Map.of("partId", resolvedPartId.toString()),
                () -> createDraft(runId, resolvedPartId, title, bodySummary, toolContext),
                executionResult -> executionResult,
                draftResult -> draftResult.requiresConfirmation()
                        ? "확인 대기 중인 이슈 초안을 만들었습니다"
                        : "대상 부품을 찾지 못했습니다",
                "이슈 초안을 만들었습니다",
                "issue_create_draft"
        );

        if (result.requiresConfirmation() && result.preview() != null) {
            chatService.publishEvent(runId, "action.required", Map.of(
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
            return "챗에서 생성한 이슈입니다.";
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
            return new IssueCreateDraftToolResult("대상 부품을 찾지 못해서 이슈 초안을 만들 수 없습니다.", false, null, null);
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
                "이슈 초안을 만들었습니다. 사용자가 확인하면 실제 생성됩니다.",
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
