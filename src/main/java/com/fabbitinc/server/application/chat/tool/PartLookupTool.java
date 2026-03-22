package com.fabbitinc.server.application.chat.tool;

import com.fabbitinc.server.application.chat.model.ChatUiArtifact;
import com.fabbitinc.server.application.chat.service.ChatToolExecutionService;
import com.fabbitinc.server.application.chat.service.ChatToolExecutionService.ToolProgressPayload;
import com.fabbitinc.server.application.chat.service.ChatService;
import com.fabbitinc.server.application.chat.support.ChatArtifactTypes;
import com.fabbitinc.server.application.chat.support.ChatMessageCatalog;
import com.fabbitinc.server.application.chat.support.ChatMessageComposer;
import com.fabbitinc.server.application.part.api.PartApi;
import com.fabbitinc.server.application.part.api.PartSnapshot;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class PartLookupTool {

    private static final String TOOL_NAME = "part_lookup";
    private static final String TOOL_DISPLAY_NAME = "품번 검색";
    private static final String TRACE_STEP = "part_lookup";

    private final PartApi partApi;
    private final ChatService chatService;
    private final ChatToolExecutionService chatToolExecutionService;
    private final ChatMessageCatalog chatMessageCatalog;
    private final ChatMessageComposer chatMessageComposer;

    public PartLookupTool(
            PartApi partApi,
            ChatService chatService,
            ChatToolExecutionService chatToolExecutionService,
            ChatMessageCatalog chatMessageCatalog,
            ChatMessageComposer chatMessageComposer
    ) {
        this.partApi = partApi;
        this.chatService = chatService;
        this.chatToolExecutionService = chatToolExecutionService;
        this.chatMessageCatalog = chatMessageCatalog;
        this.chatMessageComposer = chatMessageComposer;
    }

    @Tool(
            name = TOOL_NAME,
            description = "품번 또는 부품명으로 부품 후보를 검색합니다. 부품 관련 조회를 시작할 때 먼저 사용합니다."
    )
    public PartLookupToolResult partLookup(
            @ToolParam(description = "검색할 품번 또는 부품명") String keyword,
            @ToolParam(required = false, description = "최대 반환 개수. 비우면 5를 사용합니다.") Integer limit,
            ToolContext toolContext
    ) {
        var runId = ChatToolContextSupport.getRunId(toolContext);
        var run = chatService.getRunOrThrow(runId);
        int resolvedLimit = limit == null ? 5 : Math.max(1, Math.min(limit, 10));
        String resolvedKeyword = keyword == null ? "" : keyword.trim();
        Map<String, Object> inputPayload = Map.of(
                "keyword", resolvedKeyword,
                "limit", resolvedLimit
        );

        List<PartSnapshot> parts = chatToolExecutionService.execute(
                runId,
                run.getThreadId(),
                TOOL_NAME,
                TOOL_DISPLAY_NAME,
                inputPayload,
                () -> partApi.searchPartSnapshots(resolvedKeyword, resolvedLimit),
                result -> result,
                result -> chatMessageCatalog.partLookupSummary(result.size()),
                chatMessageCatalog.partLookupStarted(),
                result -> new ToolProgressPayload(
                        chatMessageCatalog.partLookupProgress(result.size()),
                        List.of(toPartListArtifact(result))
                ),
                chatMessageCatalog.partLookupTraceCompleted(),
                TRACE_STEP
        );
        ChatToolContextSupport.getAccumulator(toolContext).addToolName(TOOL_NAME);
        ChatToolContextSupport.getAccumulator(toolContext).addUiArtifact(toPartListArtifact(parts));

        return new PartLookupToolResult(
                chatMessageCatalog.partLookupResponseSummary(parts.size()),
                parts.stream()
                        .map(part -> new PartCandidate(
                                part.id().toString(),
                                part.revisionId() == null ? null : part.revisionId().toString(),
                                part.partNumber(),
                                part.name(),
                                part.revisionCode()
                        ))
                        .toList()
        );
    }

    public record PartLookupToolResult(
            String summary,
            List<PartCandidate> items
    ) {
    }

    public record PartCandidate(
            String id,
            String revisionId,
            String partNumber,
            String name,
            String revisionCode
    ) {
    }

    private Map<String, Object> toPartItem(PartSnapshot part) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", chatMessageComposer.stringValue(part.id()));
        item.put("revisionId", chatMessageComposer.stringValue(part.revisionId()));
        item.put("partNumber", part.partNumber());
        item.put("name", chatMessageComposer.normalizeText(part.name()));
        item.put("revisionCode", chatMessageComposer.normalizeText(part.revisionCode()));
        return item;
    }

    private ChatUiArtifact toPartListArtifact(List<PartSnapshot> parts) {
        return ChatUiArtifact.of(ChatArtifactTypes.ENTITY_LIST, chatMessageComposer.toJsonNode(Map.of(
                "entityType", "PART",
                "title", chatMessageCatalog.partListTitle(),
                "items", parts.stream()
                        .map(this::toPartItem)
                        .toList()
        )));
    }
}
