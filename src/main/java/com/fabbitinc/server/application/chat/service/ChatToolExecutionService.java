package com.fabbitinc.server.application.chat.service;

import com.fabbitinc.server.application.chat.support.ChatVisibleTraceFormatter;
import com.fabbitinc.server.domain.chat.model.ChatToolCall;
import com.fabbitinc.server.domain.chat.repository.ChatToolCallRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class ChatToolExecutionService {

    private final ChatService chatService;
    private final ChatToolCallRepository chatToolCallRepository;
    private final ChatVisibleTraceFormatter chatVisibleTraceFormatter;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public <T> T execute(
            UUID runId,
            UUID threadId,
            String toolName,
            String displayName,
            Object inputPayload,
            Supplier<T> action,
            Function<T, Object> resultPayloadMapper,
            Function<T, String> summaryMapper,
            String traceMessage,
            String traceStep
    ) {
        Timer.Sample sample = Timer.start(meterRegistry);
        ChatToolCall toolCall = chatToolCallRepository.save(
                ChatToolCall.create(runId, threadId, toolName, writeJson(inputPayload))
        );

        chatService.publishEvent(runId, "tool.started", Map.of(
                "toolName", toolName,
                "displayName", displayName,
                "input", inputPayload == null ? Map.of() : inputPayload
        ));

        try {
            T result = action.get();
            Object resultPayload = resultPayloadMapper == null ? result : resultPayloadMapper.apply(result);
            toolCall.complete(writeJson(resultPayload));
            chatToolCallRepository.save(toolCall);

            chatService.publishEvent(runId, "tool.completed", Map.of(
                    "toolName", toolName,
                    "displayName", displayName,
                    "summary", summaryMapper == null ? "완료" : summaryMapper.apply(result)
            ));
            if (traceMessage != null && !traceMessage.isBlank()) {
                chatService.publishEvent(runId, "trace.updated", chatVisibleTraceFormatter.format(
                        traceMessage,
                        traceStep,
                        "COMPLETED"
                ));
            }
            recordToolMetric(sample, toolName, "completed");
            return result;
        } catch (RuntimeException ex) {
            toolCall.fail(ex.getClass().getSimpleName(), writeJson(Map.of(
                    "message", ex.getMessage(),
                    "exception", ex.getClass().getSimpleName()
            )));
            chatToolCallRepository.save(toolCall);
            chatService.publishEvent(runId, "tool.failed", Map.of(
                    "toolName", toolName,
                    "displayName", displayName,
                    "errorCode", ex.getClass().getSimpleName()
            ));
            recordToolMetric(sample, toolName, "failed");
            throw ex;
        }
    }

    private void recordToolMetric(Timer.Sample sample, String toolName, String status) {
        meterRegistry.counter("chat.tool.calls", "tool", toolName, "status", status).increment();
        sample.stop(meterRegistry.timer("chat.tool.duration", "tool", toolName, "status", status));
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (JacksonException ex) {
            throw new IllegalStateException("tool payload 직렬화에 실패했습니다", ex);
        }
    }
}
