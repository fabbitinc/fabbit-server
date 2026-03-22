package com.fabbitinc.server.application.chat.service;

import com.fabbitinc.server.application.chat.support.ChatMessageComposer;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.domain.chat.model.ChatMessage;
import com.fabbitinc.server.domain.chat.model.ChatMessageRole;
import com.fabbitinc.server.domain.chat.repository.ChatMessageRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

@Service
@RequiredArgsConstructor
public class ChatConversationContextService {

    private static final String HISTORY_SUMMARY_TEMPLATE = "classpath:prompts/chat/history-summary.st";
    private static final int DEFAULT_HISTORY_WINDOW = 48;
    private static final int VERBATIM_HISTORY_WINDOW = 16;
    private static final int SUMMARY_ITEM_LIMIT = 12;
    private static final int SUMMARY_TEXT_LIMIT = 240;

    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageComposer chatMessageComposer;
    private final ResourceLoader resourceLoader;

    public List<Message> buildConversationMessages(ChatMessage currentUserMessage) {
        List<ChatMessage> messages = chatMessageRepository.findByThreadIdAndSequenceLessThanEqualOrderBySequenceDesc(
                currentUserMessage.getThreadId(),
                currentUserMessage.getSequence(),
                PageRequest.of(0, DEFAULT_HISTORY_WINDOW)
        );
        Collections.reverse(messages);
        int historySplitIndex = Math.max(messages.size() - VERBATIM_HISTORY_WINDOW, 0);

        List<Message> conversationMessages = new ArrayList<>();
        if (historySplitIndex > 0) {
            conversationMessages.add(new SystemMessage(buildHistorySummary(messages.subList(0, historySplitIndex))));
        }
        conversationMessages.addAll(messages.subList(historySplitIndex, messages.size()).stream()
                .map(this::toPromptMessage)
                .filter(java.util.Objects::nonNull)
                .toList());
        return conversationMessages;
    }

    private Message toPromptMessage(ChatMessage message) {
        String text = chatMessageComposer.extractText(message.getContent());
        if (text == null || text.isBlank()) {
            return null;
        }
        if (message.getRole() == ChatMessageRole.USER) {
            return new UserMessage(text);
        }
        if (message.getRole() == ChatMessageRole.ASSISTANT) {
            return new AssistantMessage(text);
        }
        if (message.getRole() == ChatMessageRole.SYSTEM) {
            return new SystemMessage(text);
        }
        return null;
    }

    private String buildHistorySummary(List<ChatMessage> messages) {
        List<ChatMessage> summarizedMessages = messages.stream()
                .filter(message -> {
                    String text = chatMessageComposer.extractText(message.getContent());
                    return text != null && !text.isBlank();
                })
                .toList();
        int startIndex = Math.max(summarizedMessages.size() - SUMMARY_ITEM_LIMIT, 0);
        StringBuilder items = new StringBuilder();
        for (ChatMessage message : summarizedMessages.subList(startIndex, summarizedMessages.size())) {
            String text = chatMessageComposer.extractText(message.getContent());
            items.append("\n- ")
                    .append(resolveSpeaker(message.getRole()))
                    .append(": ")
                    .append(truncate(text));
        }
        return readTemplate(HISTORY_SUMMARY_TEMPLATE)
                .replace("{{history_items}}", items.toString().trim());
    }

    private String resolveSpeaker(ChatMessageRole role) {
        if (role == ChatMessageRole.USER) {
            return "사용자";
        }
        if (role == ChatMessageRole.ASSISTANT) {
            return "어시스턴트";
        }
        return "시스템";
    }

    private String readTemplate(String resourceLocation) {
        try {
            return StreamUtils.copyToString(
                    resourceLoader.getResource(resourceLocation).getInputStream(),
                    StandardCharsets.UTF_8
            );
        } catch (IOException ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "챗 프롬프트 템플릿을 읽을 수 없습니다: " + resourceLocation);
        }
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= SUMMARY_TEXT_LIMIT) {
            return normalized;
        }
        return normalized.substring(0, SUMMARY_TEXT_LIMIT) + "...";
    }
}
