package com.fabbitinc.server.application.chat.service;

import com.fabbitinc.server.application.chat.model.ChatExecutionAccumulator;
import com.fabbitinc.server.application.chat.model.ChatPendingAction;
import com.fabbitinc.server.application.chat.support.ChatEventTypes;
import com.fabbitinc.server.application.chat.support.ChatInputGuard;
import com.fabbitinc.server.application.chat.support.ChatMessageCatalog;
import com.fabbitinc.server.application.chat.support.ChatMessageComposer;
import com.fabbitinc.server.application.chat.support.ChatSystemPromptFactory;
import com.fabbitinc.server.application.chat.support.ChatUsageContextHolder;
import com.fabbitinc.server.application.chat.support.ChatVisibleTraceFormatter;
import com.fabbitinc.server.application.chat.tool.ChatToolContextSupport;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.config.AppProperties;
import com.fabbitinc.server.domain.aiusage.model.AiUsageCategory;
import com.fabbitinc.server.domain.chat.model.ChatMessage;
import com.fabbitinc.server.domain.chat.model.ChatRun;
import com.fabbitinc.server.domain.chat.model.ChatThread;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatAgentService {

    private static final Pattern UUID_PATTERN =
            Pattern.compile("\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-8][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}\\b");
    private static final Pattern INTERNAL_KEY_VALUE_LINE_PATTERN =
            Pattern.compile("(?im)^\\s*[a-zA-Z_][a-zA-Z0-9_]*id\\s*:\\s*.*(?:\\R|$)");
    private static final Pattern INTERNAL_KEY_VALUE_PAREN_PATTERN =
            Pattern.compile("(?i)\\s*\\([a-zA-Z_][a-zA-Z0-9_]*id\\s*:\\s*[^)]*\\)");

    private final AppProperties appProperties;
    private final ChatClient chatClient;
    private final ChatService chatService;
    private final ChatConversationContextService chatConversationContextService;
    private final ChatMessageCatalog chatMessageCatalog;
    private final ChatMessageComposer chatMessageComposer;
    private final ChatSystemPromptFactory chatSystemPromptFactory;
    private final ChatVisibleTraceFormatter chatVisibleTraceFormatter;
    private final ChatInputGuard chatInputGuard;
    private final OpenAiChatModel openAiChatModel;
    private final ResourceLoader resourceLoader;
    private final MeterRegistry meterRegistry;

    public String getModelName() {
        return appProperties.llmModel();
    }

    public void ensureAvailable() {
        if (!appProperties.llmEnabled()) {
            throw new AppException(com.fabbitinc.server.application.common.exception.ErrorCode.PRECONDITION_FAILED, "현재 AI 챗 기능이 비활성화되어 있습니다");
        }
        if (appProperties.llmApiKey().isBlank()) {
            throw new AppException(com.fabbitinc.server.application.common.exception.ErrorCode.PRECONDITION_FAILED, "LLM API key가 설정되지 않았습니다");
        }
    }

    public void processRun(UUID runId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        ChatRun run = chatService.getRunOrThrow(runId);
        ChatThread thread = chatService.getThreadByIdOrThrow(run.getThreadId());
        ChatMessage userMessage = chatService.getMessageOrThrow(run.getUserMessageId());
        ChatMessage assistantMessage = chatService.getMessageOrThrow(run.getAssistantMessageId());
        String question = chatMessageComposer.extractText(userMessage.getContent());

        chatService.startRun(run, assistantMessage);
        chatService.publishEvent(runId, ChatEventTypes.RUN_STARTED, Map.of(
                "runId", runId.toString(),
                "status", run.getStatus().name()
        ));
        chatService.publishEvent(runId, ChatEventTypes.TRACE_UPDATED, chatVisibleTraceFormatter.format(
                chatMessageCatalog.runReasoningInProgress(),
                "reasoning",
                "IN_PROGRESS"
        ));

        try {
            ensureAvailable();
            ChatUsageContextHolder.set(thread.getOrgId(), thread.getUserId(), AiUsageCategory.CHAT, "chat:run");
            ChatInputGuard.GuardResult guardResult = chatInputGuard.check(question);
            ChatExecutionAccumulator accumulator = new ChatExecutionAccumulator();
            ChatExecutionResult result;
            if (guardResult.blocked()) {
                log.info("event=chat_run_guard_blocked run_id={} reason={}", runId, guardResult.reasonCode());
                result = executeGuardedChat(run, question, guardResult);
            } else {
                result = executeChat(run, question, accumulator);
            }
            String assistantContent = composeAssistantContent(result.text(), accumulator);

            chatService.publishEvent(run.getId(), ChatEventTypes.MESSAGE_COMPLETED, Map.of(
                    "messageId", assistantMessage.getId().toString()
            ));

            ChatPendingAction pendingAction = accumulator.getPendingAction();
            if (pendingAction != null) {
                chatService.waitForConfirmation(run, assistantMessage, assistantContent, chatMessageComposer.writeMap(Map.of(
                        "responseId", result.responseId(),
                        "toolNames", result.toolNames(),
                        "actionRequestId", pendingAction.actionRequest().getId().toString()
                )));
                chatService.publishEvent(run.getId(), ChatEventTypes.RUN_WAITING_CONFIRMATION, Map.of(
                        "runId", run.getId().toString(),
                        "status", "WAITING_CONFIRMATION",
                        "actionRequestId", pendingAction.actionRequest().getId().toString()
                ));
                chatService.publishEvent(run.getId(), ChatEventTypes.RUN_COMPLETED, Map.of(
                        "runId", run.getId().toString(),
                        "status", "WAITING_CONFIRMATION",
                        "actionRequestId", pendingAction.actionRequest().getId().toString()
                ));
                recordRunMetric(sample, "waiting_confirmation");
                return;
            }

            chatService.publishEvent(run.getId(), ChatEventTypes.RUN_COMPLETED, Map.of(
                    "runId", run.getId().toString(),
                    "status", "COMPLETED"
            ));
            chatService.completeRun(run, assistantMessage, assistantContent, result.inputTokens(), result.outputTokens(), chatMessageComposer.writeMap(Map.of(
                    "responseId", result.responseId(),
                    "toolNames", result.toolNames()
            )));
            recordRunMetric(sample, "completed");
        } catch (RuntimeException ex) {
            String errorMessage = chatMessageCatalog.chatAgentFailed();
            log.error("event=chat_run_failed run_id={} thread_id={} reason=agent_exception", runId, run.getThreadId(), ex);
            chatService.publishEvent(runId, ChatEventTypes.RUN_FAILED, Map.of(
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
                    chatMessageComposer.writeMap(Map.of(
                            "exception", ex.getClass().getSimpleName(),
                            "message", ex.getMessage() == null ? "" : ex.getMessage()
                    ))
            );
            recordRunMetric(sample, "failed");
        } finally {
            ChatUsageContextHolder.clear();
        }
    }

    private ChatExecutionResult executeChat(ChatRun run, String question, ChatExecutionAccumulator accumulator) {
        List<Message> conversationMessages = chatConversationContextService.buildConversationMessages(
                chatService.getMessageOrThrow(run.getUserMessageId())
        );
        ChatResponse response = chatClient.prompt()
                .system(chatSystemPromptFactory.create())
                .messages(conversationMessages)
                .toolContext(ChatToolContextSupport.createContext(run.getId(), question, accumulator))
                .call()
                .chatResponse();

        String text = response == null || response.getResult() == null || response.getResult().getOutput() == null
                ? ""
                : response.getResult().getOutput().getText();
        Usage usage = response == null ? null : response.getMetadata().getUsage();
        String responseId = response == null ? "" : response.getMetadata().getId();
        return new ChatExecutionResult(
                text == null ? "" : text,
                usage == null || usage.getPromptTokens() == null ? 0 : usage.getPromptTokens(),
                usage == null || usage.getCompletionTokens() == null ? 0 : usage.getCompletionTokens(),
                response == null || response.getMetadata() == null || response.getMetadata().getModel() == null
                        ? appProperties.llmModel()
                        : response.getMetadata().getModel(),
                responseId == null ? "" : responseId,
                accumulator.getToolNames().stream().toList()
        );
    }

    private static final String GUARD_REJECT_TEMPLATE = "classpath:prompts/chat/guard-reject.st";

    private ChatExecutionResult executeGuardedChat(ChatRun run, String question, ChatInputGuard.GuardResult guardResult) {
        String rejectSystemPrompt = readPromptTemplate(GUARD_REJECT_TEMPLATE)
                .replace("{{reason_code}}", guardResult.reasonCode());

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(appProperties.llmGuardModel())
                .temperature(0.2)
                .maxTokens(200)
                .build();

        ChatResponse response = openAiChatModel.call(
                new org.springframework.ai.chat.prompt.Prompt(List.of(
                        new org.springframework.ai.chat.messages.SystemMessage(rejectSystemPrompt),
                        new UserMessage(question)
                ), options)
        );

        String text = response.getResult() == null
                ? chatMessageCatalog.chatGuardBlocked()
                : response.getResult().getOutput().getText();
        Usage usage = response == null ? null : response.getMetadata().getUsage();
        String responseId = response == null ? "" : response.getMetadata().getId();
        return new ChatExecutionResult(
                text == null ? chatMessageCatalog.chatGuardBlocked() : text,
                usage == null || usage.getPromptTokens() == null ? 0 : usage.getPromptTokens(),
                usage == null || usage.getCompletionTokens() == null ? 0 : usage.getCompletionTokens(),
                response == null || response.getMetadata() == null || response.getMetadata().getModel() == null
                        ? appProperties.llmModel()
                        : response.getMetadata().getModel(),
                responseId == null ? "" : responseId,
                List.of()
        );
    }

    private String readPromptTemplate(String resourceLocation) {
        try {
            return StreamUtils.copyToString(
                    resourceLoader.getResource(resourceLocation).getInputStream(),
                    StandardCharsets.UTF_8
            );
        } catch (IOException ex) {
            return "";
        }
    }

    private String composeAssistantContent(String text, ChatExecutionAccumulator accumulator) {
        String resolvedText = normalizeAssistantText(text, accumulator);
        return chatMessageComposer.assistantStructured(resolvedText, resolveFinalArtifacts(accumulator));
    }

    private String normalizeAssistantText(String text, ChatExecutionAccumulator accumulator) {
        if (accumulator.getPendingAction() != null) {
            return chatMessageCatalog.pendingIssueDraftReady();
        }
        if (text != null && !text.isBlank()) {
            return sanitizeAssistantText(text);
        }
        if (!accumulator.getToolNames().isEmpty()) {
            return chatMessageCatalog.resultsPrepared();
        }
        return chatMessageCatalog.requestProcessed();
    }

    private void recordRunMetric(Timer.Sample sample, String status) {
        meterRegistry.counter("chat.run.calls", "status", status).increment();
        sample.stop(meterRegistry.timer("chat.run.duration", "status", status));
    }

    private String sanitizeAssistantText(String text) {
        String sanitized = text == null ? "" : text.trim();
        sanitized = INTERNAL_KEY_VALUE_LINE_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = INTERNAL_KEY_VALUE_PAREN_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = UUID_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = sanitized.replaceAll("\\n{3,}", "\n\n").trim();
        if (sanitized.isBlank()) {
            return chatMessageCatalog.sanitizedFallback();
        }
        return sanitized;
    }

    private List<com.fabbitinc.server.application.chat.model.ChatUiArtifact> resolveFinalArtifacts(
            ChatExecutionAccumulator accumulator
    ) {
        ChatPendingAction pendingAction = accumulator.getPendingAction();
        if (pendingAction != null) {
            return List.of(pendingAction.uiArtifact());
        }
        return accumulator.getUiArtifacts();
    }

    private record ChatExecutionResult(
            String text,
            int inputTokens,
            int outputTokens,
            String model,
            String responseId,
            java.util.List<String> toolNames
    ) {
    }
}
