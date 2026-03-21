package com.fabbitinc.server.application.chat.service;

import com.fabbitinc.server.application.chat.support.ChatMessageComposer;
import com.fabbitinc.server.application.chat.support.ChatToolRegistry;
import com.fabbitinc.server.application.chat.support.ChatVisibleTraceFormatter;
import com.fabbitinc.server.application.issue.api.IssueSnapshot;
import com.fabbitinc.server.application.part.api.PartSnapshot;
import com.fabbitinc.server.domain.chat.model.ChatActionRequest;
import com.fabbitinc.server.domain.chat.model.ChatActionRequestType;
import com.fabbitinc.server.domain.chat.model.ChatIntent;
import com.fabbitinc.server.domain.chat.model.ChatRun;
import com.fabbitinc.server.domain.chat.model.ChatMessage;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatAgentService {

    private static final Pattern PART_TOKEN_PATTERN = Pattern.compile("([A-Za-z0-9][A-Za-z0-9._-]{1,})");

    private final ChatService chatService;
    private final ChatToolRegistry chatToolRegistry;
    private final ChatActionService chatActionService;
    private final ChatMessageComposer chatMessageComposer;
    private final ChatVisibleTraceFormatter chatVisibleTraceFormatter;

    public ChatIntent detectIntent(String text) {
        String normalized = text == null ? "" : text.toLowerCase();
        if ((normalized.contains("이슈") || normalized.contains("issue"))
                && (normalized.contains("생성") || normalized.contains("만들") || normalized.contains("등록"))) {
            return ChatIntent.ISSUE_CREATE_DRAFT;
        }
        if (normalized.contains("이슈")) {
            return ChatIntent.PART_ISSUE_LOOKUP;
        }
        if (normalized.contains("품번") || normalized.contains("부품") || normalized.contains("찾")) {
            return ChatIntent.PART_LOOKUP;
        }
        return ChatIntent.GENERAL_CHAT;
    }

    public void processRun(UUID runId) {
        ChatRun run = chatService.getRunOrThrow(runId);
        ChatMessage userMessage = chatService.getMessageOrThrow(run.getUserMessageId());
        ChatMessage assistantMessage = chatService.getMessageOrThrow(run.getAssistantMessageId());
        String question = chatMessageComposer.extractText(userMessage.getContent());

        chatService.startRun(run, assistantMessage);
        chatService.publishEvent(runId, "run.started", Map.of(
                "runId", runId.toString(),
                "status", run.getStatus().name()
        ));
        chatService.publishEvent(runId, "trace.updated", chatVisibleTraceFormatter.format(
                "질문 의도를 분석했습니다",
                "intent_detection",
                "COMPLETED"
        ));

        try {
            switch (run.getIntent()) {
                case PART_LOOKUP -> processPartLookup(run, assistantMessage, question);
                case PART_ISSUE_LOOKUP -> processPartIssueLookup(run, assistantMessage, question);
                case ISSUE_CREATE_DRAFT -> processIssueCreateDraft(run, assistantMessage, question);
                case GENERAL_CHAT -> processGeneralChat(run, assistantMessage, question);
            }
        } catch (RuntimeException ex) {
            String errorMessage = "챗 응답 생성 중 오류가 발생했습니다.";
            chatService.publishEvent(runId, "run.failed", Map.of(
                    "runId", runId.toString(),
                    "status", "FAILED",
                    "errorCode", "CHAT_AGENT_FAILED",
                    "message", errorMessage
            ));
            chatService.failRun(
                    run,
                    assistantMessage,
                    "CHAT_AGENT_FAILED",
                    chatMessageComposer.errorText(errorMessage),
                    chatMessageComposer.writeMap(Map.of("exception", ex.getClass().getSimpleName()))
            );
        }
    }

    private void processPartLookup(ChatRun run, ChatMessage assistantMessage, String question) {
        String keyword = extractKeyword(question);
        chatService.publishEvent(run.getId(), "tool.started", Map.of(
                "toolName", "part_lookup",
                "displayName", "품번 검색",
                "input", Map.of("keyword", keyword)
        ));
        List<PartSnapshot> parts = chatToolRegistry.searchPartSnapshots(keyword, 10);
        chatService.publishEvent(run.getId(), "tool.completed", Map.of(
                "toolName", "part_lookup",
                "displayName", "품번 검색",
                "summary", "후보 " + parts.size() + "건을 찾았습니다"
        ));
        chatService.publishEvent(run.getId(), "trace.updated", chatVisibleTraceFormatter.format(
                "품번 후보를 조회했습니다",
                "part_lookup",
                "COMPLETED"
        ));

        String text;
        if (parts.isEmpty()) {
            text = "일치하는 품번 후보를 찾지 못했습니다. 더 구체적인 품번이나 품명을 입력해 주세요.";
        } else {
            text = "품번 후보 " + parts.size() + "건을 찾았습니다.";
        }
        String assistantContent = chatMessageComposer.partLookupResult(text, parts, List.of());
        chatService.publishEvent(run.getId(), "message.completed", Map.of(
                "messageId", assistantMessage.getId().toString()
        ));
        chatService.publishEvent(run.getId(), "run.completed", Map.of(
                "runId", run.getId().toString(),
                "status", "COMPLETED"
        ));
        chatService.completeRun(run, assistantMessage, assistantContent, 0, 0, chatMessageComposer.writeMap(Map.of(
                "keyword", keyword,
                "partCount", parts.size()
        )));
    }

    private void processPartIssueLookup(ChatRun run, ChatMessage assistantMessage, String question) {
        String keyword = extractKeyword(question);
        List<PartSnapshot> parts = chatToolRegistry.searchPartSnapshots(keyword, 5);
        List<IssueSnapshot> issues = parts.size() == 1
                ? chatToolRegistry.getIssueSnapshotsByPartIds(Set.of(parts.get(0).id()))
                : List.of();

        chatService.publishEvent(run.getId(), "trace.updated", chatVisibleTraceFormatter.format(
                "부품과 연결된 이슈를 조회했습니다",
                "issue_lookup",
                "COMPLETED"
        ));

        String text;
        if (parts.isEmpty()) {
            text = "대상 부품을 찾지 못해서 연결된 이슈를 조회할 수 없습니다.";
        } else if (parts.size() > 1) {
            text = "후보 부품이 여러 개라서 먼저 대상 부품을 좁혀야 합니다.";
        } else {
            text = "부품과 연결된 이슈 " + issues.size() + "건을 찾았습니다.";
        }
        String assistantContent = chatMessageComposer.partLookupResult(text, parts, issues);
        chatService.publishEvent(run.getId(), "message.completed", Map.of(
                "messageId", assistantMessage.getId().toString()
        ));
        chatService.publishEvent(run.getId(), "run.completed", Map.of(
                "runId", run.getId().toString(),
                "status", "COMPLETED"
        ));
        chatService.completeRun(run, assistantMessage, assistantContent, 0, 0, chatMessageComposer.writeMap(Map.of(
                "keyword", keyword,
                "partCount", parts.size(),
                "issueCount", issues.size()
        )));
    }

    private void processIssueCreateDraft(ChatRun run, ChatMessage assistantMessage, String question) {
        String keyword = extractKeyword(question);
        List<PartSnapshot> parts = chatToolRegistry.searchPartSnapshots(keyword, 5);
        if (parts.isEmpty()) {
            completeNoTargetDraft(run, assistantMessage, "대상 부품을 찾지 못해서 이슈 초안을 만들 수 없습니다.");
            return;
        }
        if (parts.size() > 1) {
            completeNoTargetDraft(run, assistantMessage, "후보 부품이 여러 개라서 먼저 대상 부품을 하나로 좁혀야 합니다.");
            return;
        }

        PartSnapshot target = parts.get(0);
        ChatActionRequest actionRequest = chatActionService.createIssueDraft(run, target.id(), target.partNumber(), question);
        String assistantText = "이슈 초안을 만들었습니다. 확인 후 실행할 수 있습니다.";
        String assistantContent = chatMessageComposer.issueDraftResult(
                assistantText,
                actionRequest,
                chatMessageComposer.parse(actionRequest.getPreviewPayload())
        );

        chatService.publishEvent(run.getId(), "trace.updated", chatVisibleTraceFormatter.format(
                "이슈 초안을 만들었습니다",
                "issue_create_draft",
                "COMPLETED"
        ));
        chatService.publishEvent(run.getId(), "action.required", Map.of(
                "actionRequestId", actionRequest.getId().toString(),
                "actionType", ChatActionRequestType.CREATE_ISSUE.name(),
                "preview", chatMessageComposer.parse(actionRequest.getPreviewPayload())
        ));
        chatService.publishEvent(run.getId(), "message.completed", Map.of(
                "messageId", assistantMessage.getId().toString()
        ));
        chatService.waitForConfirmation(run, assistantMessage, assistantContent, chatMessageComposer.writeMap(Map.of(
                "keyword", keyword,
                "partId", target.id().toString(),
                "actionRequestId", actionRequest.getId().toString()
        )));
    }

    private void processGeneralChat(ChatRun run, ChatMessage assistantMessage, String question) {
        String text = """
                현재 1차 챗 스캐폴딩이 적용되어 있습니다.
                품번 검색, 부품 연결 이슈 조회, 이슈 초안 생성 요청을 우선 지원합니다.
                """.trim();
        String assistantContent = chatMessageComposer.assistantText(text);
        chatService.publishEvent(run.getId(), "message.completed", Map.of(
                "messageId", assistantMessage.getId().toString()
        ));
        chatService.publishEvent(run.getId(), "run.completed", Map.of(
                "runId", run.getId().toString(),
                "status", "COMPLETED"
        ));
        chatService.completeRun(run, assistantMessage, assistantContent, 0, 0, chatMessageComposer.writeMap(Map.of(
                "question", question
        )));
    }

    private void completeNoTargetDraft(ChatRun run, ChatMessage assistantMessage, String text) {
        String assistantContent = chatMessageComposer.assistantText(text);
        chatService.publishEvent(run.getId(), "message.completed", Map.of(
                "messageId", assistantMessage.getId().toString()
        ));
        chatService.publishEvent(run.getId(), "run.completed", Map.of(
                "runId", run.getId().toString(),
                "status", "COMPLETED"
        ));
        chatService.completeRun(run, assistantMessage, assistantContent, 0, 0, "{}");
    }

    private String extractKeyword(String question) {
        if (question == null || question.isBlank()) {
            return "";
        }
        Matcher matcher = PART_TOKEN_PATTERN.matcher(question);
        Set<String> candidates = new LinkedHashSet<>();
        while (matcher.find()) {
            String token = matcher.group(1);
            if (token.length() < 2) {
                continue;
            }
            candidates.add(token);
        }
        return candidates.stream()
                .filter(this::looksLikePartToken)
                .findFirst()
                .orElseGet(() -> candidates.stream().findFirst().orElse(question.trim()));
    }

    private boolean looksLikePartToken(String token) {
        boolean hasDigit = token.chars().anyMatch(Character::isDigit);
        boolean hasSeparator = token.contains("-") || token.contains("_") || token.contains(".");
        return hasDigit || hasSeparator;
    }
}
