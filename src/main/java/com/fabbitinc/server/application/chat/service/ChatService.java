package com.fabbitinc.server.application.chat.service;

import com.fabbitinc.server.application.chat.support.ChatSsePublisher;
import com.fabbitinc.server.application.chat.support.ChatSseEventWriter;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.chat.model.ChatActionRequest;
import com.fabbitinc.server.domain.chat.model.ChatActionRequestType;
import com.fabbitinc.server.domain.chat.model.ChatIntent;
import com.fabbitinc.server.domain.chat.model.ChatMessage;
import com.fabbitinc.server.domain.chat.model.ChatRun;
import com.fabbitinc.server.domain.chat.model.ChatRunEvent;
import com.fabbitinc.server.domain.chat.model.ChatRunEventVisibility;
import com.fabbitinc.server.domain.chat.model.ChatThread;
import com.fabbitinc.server.domain.chat.repository.ChatActionRequestRepository;
import com.fabbitinc.server.domain.chat.repository.ChatMessageRepository;
import com.fabbitinc.server.domain.chat.repository.ChatRunEventRepository;
import com.fabbitinc.server.domain.chat.repository.ChatRunRepository;
import com.fabbitinc.server.domain.chat.repository.ChatThreadRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatThreadRepository chatThreadRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRunRepository chatRunRepository;
    private final ChatRunEventRepository chatRunEventRepository;
    private final ChatActionRequestRepository chatActionRequestRepository;
    private final ChatSsePublisher chatSsePublisher;
    private final ChatSseEventWriter chatSseEventWriter;

    public ChatThread createThread(
            UUID orgId,
            UUID userId,
            UUID projectId,
            String contextType,
            UUID contextId,
            String title
    ) {
        ChatThread thread = ChatThread.create(orgId, userId, projectId, contextType, contextId, title);
        return chatThreadRepository.save(thread);
    }

    public ChatThread getThreadOrThrow(UUID threadId, UUID orgId, UUID userId) {
        return chatThreadRepository.findByIdAndOrgIdAndUserId(threadId, orgId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "챗 스레드를 찾을 수 없습니다"));
    }

    public ChatThread getThreadByIdOrThrow(UUID threadId) {
        return chatThreadRepository.findById(threadId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "챗 스레드를 찾을 수 없습니다"));
    }

    public ChatThread getThreadForUpdateOrThrow(UUID threadId, UUID orgId, UUID userId) {
        return chatThreadRepository.findByIdAndOrgIdAndUserIdForUpdate(threadId, orgId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "챗 스레드를 찾을 수 없습니다"));
    }

    public ChatRun getRunOrThrow(UUID runId) {
        return chatRunRepository.findById(runId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "챗 실행 정보를 찾을 수 없습니다"));
    }

    public ChatRun getRunForUpdateOrThrow(UUID runId) {
        return chatRunRepository.findByIdForUpdate(runId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "챗 실행 정보를 찾을 수 없습니다"));
    }

    public ChatMessage getMessageOrThrow(UUID messageId) {
        return chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "챗 메시지를 찾을 수 없습니다"));
    }

    public ChatActionRequest getActionRequestOrThrow(UUID actionRequestId) {
        return chatActionRequestRepository.findById(actionRequestId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "액션 요청을 찾을 수 없습니다"));
    }

    public ChatActionRequest getActionRequestForUpdateOrThrow(UUID actionRequestId) {
        return chatActionRequestRepository.findByIdForUpdate(actionRequestId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "액션 요청을 찾을 수 없습니다"));
    }

    public PreparedRun prepareRun(ChatThread thread, String userMessageContent, String model, ChatIntent intent, String metadata) {
        thread.ensureActive();
        Instant now = Instant.now();

        ChatMessage userMessage = chatMessageRepository.save(
                ChatMessage.createUserMessage(thread.getId(), userMessageContent, nextMessageSequence(thread.getId()))
        );
        ChatRun run = chatRunRepository.save(ChatRun.create(thread.getId(), userMessage.getId(), model, intent, metadata));
        ChatMessage assistantMessage = chatMessageRepository.save(
                ChatMessage.createAssistantMessage(thread.getId(), run.getId(), "{\"text\":\"\"}", nextMessageSequence(thread.getId()))
        );
        run.attachAssistantMessage(assistantMessage.getId());
        chatRunRepository.save(run);
        thread.touchLastMessageAt(now);
        chatThreadRepository.save(thread);
        return new PreparedRun(thread, userMessage, run, assistantMessage);
    }

    public void startRun(ChatRun run, ChatMessage assistantMessage) {
        run.start();
        assistantMessage.startStreaming();
        chatRunRepository.save(run);
        chatMessageRepository.save(assistantMessage);
    }

    public void completeRun(ChatRun run, ChatMessage assistantMessage, String assistantContent, int inputTokens, int outputTokens, String metadata) {
        assistantMessage.complete(assistantContent);
        run.complete(inputTokens, outputTokens, metadata);
        chatMessageRepository.save(assistantMessage);
        chatRunRepository.save(run);
        touchThreadLastMessageAt(run.getThreadId());
    }

    public void waitForConfirmation(ChatRun run, ChatMessage assistantMessage, String assistantContent, String metadata) {
        assistantMessage.complete(assistantContent);
        run.waitForConfirmation(metadata);
        chatMessageRepository.save(assistantMessage);
        chatRunRepository.save(run);
        touchThreadLastMessageAt(run.getThreadId());
    }

    public void failRun(ChatRun run, ChatMessage assistantMessage, String errorCode, String assistantContent, String metadata) {
        assistantMessage.fail(assistantContent);
        run.fail(errorCode, metadata);
        chatMessageRepository.save(assistantMessage);
        chatRunRepository.save(run);
        touchThreadLastMessageAt(run.getThreadId());
    }

    public ChatActionRequest createActionRequest(
            ChatRun run,
            ChatActionRequestType actionType,
            String previewPayload,
            String requestPayload,
            Instant expiresAt
    ) {
        ChatActionRequest actionRequest = ChatActionRequest.create(
                run.getId(),
                run.getThreadId(),
                actionType,
                previewPayload,
                requestPayload,
                expiresAt
        );
        return chatActionRequestRepository.save(actionRequest);
    }

    public ChatMessage appendAssistantNotice(UUID threadId, String content) {
        chatThreadRepository.findByIdForUpdate(threadId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "챗 스레드를 찾을 수 없습니다"));
        ChatMessage message = ChatMessage.createAssistantNotice(threadId, content, nextMessageSequence(threadId));
        ChatMessage saved = chatMessageRepository.save(message);
        touchThreadLastMessageAt(threadId);
        return saved;
    }

    public List<ChatRunEvent> getRunEvents(UUID runId) {
        return chatRunEventRepository.findByRunIdOrderBySequenceAsc(runId);
    }

    public List<ChatRunEvent> getRunEventsAfter(UUID runId, long lastSequence) {
        return chatRunEventRepository.findByRunIdAndSequenceGreaterThanOrderBySequenceAsc(runId, lastSequence);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishEvent(UUID runId, String eventType, Map<String, Object> payload) {
        publishEvent(runId, eventType, payload, ChatRunEventVisibility.USER_VISIBLE);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishEvent(UUID runId, String eventType, Map<String, Object> payload, ChatRunEventVisibility visibility) {
        getRunForUpdateOrThrow(runId);
        long sequence = chatRunEventRepository.countByRunId(runId) + 1;
        String payloadJson = chatSseEventWriter.serialize(payload);
        ChatRunEvent event = ChatRunEvent.create(runId, sequence, eventType, visibility, payloadJson);
        chatRunEventRepository.save(event);
        chatSsePublisher.push(runId, chatSseEventWriter.write(event.getSequence(), eventType, payload));
    }

    private void touchThreadLastMessageAt(UUID threadId) {
        chatThreadRepository.findById(threadId).ifPresent(thread -> {
            thread.touchLastMessageAt(Instant.now());
            chatThreadRepository.save(thread);
        });
    }

    private long nextMessageSequence(UUID threadId) {
        return chatMessageRepository.countByThreadId(threadId) + 1L;
    }

    public record PreparedRun(
            ChatThread thread,
            ChatMessage userMessage,
            ChatRun run,
            ChatMessage assistantMessage
    ) {
    }
}
