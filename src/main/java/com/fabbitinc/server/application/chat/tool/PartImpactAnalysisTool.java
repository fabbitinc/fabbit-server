package com.fabbitinc.server.application.chat.tool;

import com.fabbitinc.server.application.chat.model.ChatUiArtifact;
import com.fabbitinc.server.application.chat.service.ChatService;
import com.fabbitinc.server.application.chat.service.ChatToolExecutionService;
import com.fabbitinc.server.application.chat.service.ChatToolExecutionService.ToolProgressPayload;
import com.fabbitinc.server.application.chat.support.ChatArtifactTypes;
import com.fabbitinc.server.application.chat.support.ChatMessageCatalog;
import com.fabbitinc.server.application.chat.support.ChatMessageComposer;
import com.fabbitinc.server.application.part.query.PartImpactAnalysisQuery;
import com.fabbitinc.server.application.part.query.condition.PartImpactAnalysisCondition;
import com.fabbitinc.server.application.part.query.result.PartImpactAnalysisResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class PartImpactAnalysisTool {

    private static final String TOOL_NAME = "part_impact_analysis";
    private static final String TOOL_DISPLAY_NAME = "부품 영향 분석";
    private static final String TRACE_STEP = "part_impact_analysis";

    private final PartImpactAnalysisQuery partImpactAnalysisQuery;
    private final ChatService chatService;
    private final ChatToolExecutionService chatToolExecutionService;
    private final ChatMessageCatalog chatMessageCatalog;
    private final ChatMessageComposer chatMessageComposer;

    public PartImpactAnalysisTool(
            PartImpactAnalysisQuery partImpactAnalysisQuery,
            ChatService chatService,
            ChatToolExecutionService chatToolExecutionService,
            ChatMessageCatalog chatMessageCatalog,
            ChatMessageComposer chatMessageComposer
    ) {
        this.partImpactAnalysisQuery = partImpactAnalysisQuery;
        this.chatService = chatService;
        this.chatToolExecutionService = chatToolExecutionService;
        this.chatMessageCatalog = chatMessageCatalog;
        this.chatMessageComposer = chatMessageComposer;
    }

    @Tool(
            name = TOOL_NAME,
            description = """
                    특정 부품을 변경했을 때 상위 BOM, 프로젝트, 추천 리뷰어에 어떤 영향이 가는지 조회할 때 사용합니다.
                    이미 확인된 부품 ID가 있을 때만 사용하며 읽기 전용 조회 도구입니다.
                    """
    )
    public PartImpactAnalysisToolResult partImpactAnalysis(
            @ToolParam(description = "조회할 부품 ID") String partId,
            ToolContext toolContext
    ) {
        UUID resolvedPartId = UUID.fromString(partId);
        var runId = ChatToolContextSupport.getRunId(toolContext);
        var run = chatService.getRunOrThrow(runId);

        PartImpactAnalysisResult result = chatToolExecutionService.execute(
                runId,
                run.getThreadId(),
                TOOL_NAME,
                TOOL_DISPLAY_NAME,
                Map.of("partId", resolvedPartId.toString()),
                () -> partImpactAnalysisQuery.analyze(new PartImpactAnalysisCondition(resolvedPartId)),
                analysis -> analysis,
                analysis -> "영향 BOM " + analysis.summary().affectedBomCount() + "건, 프로젝트 "
                        + analysis.summary().affectedProjectCount() + "건을 찾았습니다",
                chatMessageCatalog.partImpactAnalysisStarted(),
                analysis -> new ToolProgressPayload(
                        "영향 BOM " + analysis.summary().affectedBomCount() + "건, 프로젝트 "
                                + analysis.summary().affectedProjectCount() + "건을 찾았습니다.",
                        List.of(toImpactArtifact(analysis))
                ),
                chatMessageCatalog.partImpactAnalysisTraceCompleted(),
                TRACE_STEP
        );

        ChatToolContextSupport.getAccumulator(toolContext).addToolName(TOOL_NAME);
        ChatToolContextSupport.getAccumulator(toolContext).addUiArtifact(toImpactArtifact(result));

        return new PartImpactAnalysisToolResult(
                "영향 BOM %d건, 프로젝트 %d건, 수정 필요 DRAFT 리비전 %d건입니다."
                        .formatted(
                                result.summary().affectedBomCount(),
                                result.summary().affectedProjectCount(),
                                result.summary().draftRevisionCount()
                        ),
                result.summary().affectedBomCount(),
                result.summary().affectedProjectCount(),
                result.summary().draftRevisionCount(),
                result.summary().suggestedReviewerIds().stream().map(UUID::toString).toList(),
                result.summary().truncated(),
                result.summary().totalCount()
        );
    }

    public record PartImpactAnalysisToolResult(
            String summary,
            int affectedBomCount,
            int affectedProjectCount,
            int draftRevisionCount,
            List<String> suggestedReviewerIds,
            boolean truncated,
            int totalCount
    ) {
    }

    private ChatUiArtifact toImpactArtifact(PartImpactAnalysisResult result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("entityType", "PART_IMPACT_ANALYSIS");
        payload.put("title", "부품 영향 분석");
        payload.put("summary", Map.of(
                "affectedBomCount", result.summary().affectedBomCount(),
                "affectedProjectCount", result.summary().affectedProjectCount(),
                "draftRevisionCount", result.summary().draftRevisionCount(),
                "truncated", result.summary().truncated(),
                "totalCount", result.summary().totalCount()
        ));
        payload.put("items", result.bomItems().stream().map(item -> {
            Map<String, Object> mapped = new LinkedHashMap<>();
            mapped.put("parentPartId", chatMessageComposer.stringValue(item.parentPartId()));
            mapped.put("parentPartNumber", item.parentPartNumber());
            mapped.put("parentPartName", chatMessageComposer.normalizeText(item.parentPartName()));
            mapped.put("parentRevisionCode", chatMessageComposer.normalizeText(item.parentRevisionCode()));
            mapped.put("level", item.level());
            return mapped;
        }).toList());
        return ChatUiArtifact.of(ChatArtifactTypes.ENTITY_LIST, chatMessageComposer.toJsonNode(payload));
    }
}
