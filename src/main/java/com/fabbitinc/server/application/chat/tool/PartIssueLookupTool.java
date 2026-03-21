package com.fabbitinc.server.application.chat.tool;

import com.fabbitinc.server.application.chat.model.ChatUiArtifact;
import com.fabbitinc.server.application.chat.service.ChatService;
import com.fabbitinc.server.application.chat.service.ChatToolExecutionService;
import com.fabbitinc.server.application.chat.support.ChatMessageComposer;
import com.fabbitinc.server.application.issue.api.IssueApi;
import com.fabbitinc.server.application.issue.api.IssueSnapshot;
import com.fabbitinc.server.application.part.api.PartApi;
import com.fabbitinc.server.application.part.api.PartSnapshot;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class PartIssueLookupTool {

    private final PartApi partApi;
    private final IssueApi issueApi;
    private final ChatService chatService;
    private final ChatToolExecutionService chatToolExecutionService;
    private final ChatMessageComposer chatMessageComposer;

    public PartIssueLookupTool(
            PartApi partApi,
            IssueApi issueApi,
            ChatService chatService,
            ChatToolExecutionService chatToolExecutionService,
            ChatMessageComposer chatMessageComposer
    ) {
        this.partApi = partApi;
        this.issueApi = issueApi;
        this.chatService = chatService;
        this.chatToolExecutionService = chatToolExecutionService;
        this.chatMessageComposer = chatMessageComposer;
    }

    @Tool(
            name = "part_issue_lookup",
            description = "특정 부품과 연결된 이슈 목록을 조회합니다. part_lookup으로 부품을 먼저 찾은 뒤 partId를 전달해 사용합니다."
    )
    public PartIssueLookupToolResult partIssueLookup(
            @ToolParam(description = "조회할 부품 ID") String partId,
            ToolContext toolContext
    ) {
        UUID resolvedPartId = UUID.fromString(partId);
        var runId = ChatToolContextSupport.getRunId(toolContext);
        var run = chatService.getRunOrThrow(runId);

        PartIssueLookupPayload payload = chatToolExecutionService.execute(
                runId,
                run.getThreadId(),
                "part_issue_lookup",
                "연결 이슈 조회",
                Map.of("partId", resolvedPartId.toString()),
                () -> {
                    PartSnapshot part = partApi.getPartSnapshotMap(Set.of(resolvedPartId)).get(resolvedPartId);
                    List<IssueSnapshot> issues = part == null ? List.of() : getIssueSnapshots(resolvedPartId);
                    return new PartIssueLookupPayload(part, issues);
                },
                result -> result,
                result -> "연결 이슈 " + result.issues().size() + "건을 찾았습니다",
                "부품과 연결된 이슈를 조회했습니다",
                "issue_lookup"
        );

        PartSnapshot part = payload.part();
        List<IssueSnapshot> issues = payload.issues();
        ChatToolContextSupport.getAccumulator(toolContext).addToolName("part_issue_lookup");
        if (part != null) {
            ChatToolContextSupport.getAccumulator(toolContext).addUiArtifact(toPartDetailArtifact(part));
        }
        ChatToolContextSupport.getAccumulator(toolContext).addUiArtifact(toIssueListArtifact(issues));

        return new PartIssueLookupToolResult(
                part == null ? "대상 부품을 찾지 못했습니다." : "연결 이슈 " + issues.size() + "건을 찾았습니다.",
                part == null ? null : new PartCandidate(
                        part.id().toString(),
                        part.revisionId() == null ? null : part.revisionId().toString(),
                        part.partNumber(),
                        part.name(),
                        part.revisionCode()
                ),
                issues.stream()
                        .map(issue -> new IssueCandidate(
                                issue.id().toString(),
                                issue.number(),
                                issue.title(),
                                issue.state().name()
                        ))
                        .toList()
        );
    }

    private List<IssueSnapshot> getIssueSnapshots(UUID partId) {
        Set<UUID> issueIds = issueApi.getIssueIdsByPartIds(Set.of(partId));
        return new LinkedHashSet<>(issueApi.getIssueSnapshotMap(issueIds).values()).stream()
                .sorted(Comparator.comparing(IssueSnapshot::number).reversed())
                .toList();
    }

    public record PartIssueLookupToolResult(
            String summary,
            PartCandidate part,
            List<IssueCandidate> items
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

    public record IssueCandidate(
            String id,
            int number,
            String title,
            String state
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

    private Map<String, Object> toIssueItem(IssueSnapshot issue) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", chatMessageComposer.stringValue(issue.id()));
        item.put("number", issue.number());
        item.put("title", issue.title());
        item.put("state", issue.state().name());
        return item;
    }

    private ChatUiArtifact toPartDetailArtifact(PartSnapshot part) {
        return ChatUiArtifact.of("entity_detail", chatMessageComposer.toJsonNode(Map.of(
                "entityType", "PART",
                "title", "대상 부품",
                "item", toPartItem(part)
        )));
    }

    private ChatUiArtifact toIssueListArtifact(List<IssueSnapshot> issues) {
        return ChatUiArtifact.of("entity_list", chatMessageComposer.toJsonNode(Map.of(
                "entityType", "ISSUE",
                "title", "연결 이슈",
                "items", issues.stream()
                        .map(this::toIssueItem)
                        .toList()
        )));
    }

    private record PartIssueLookupPayload(
            PartSnapshot part,
            List<IssueSnapshot> issues
    ) {
    }
}
