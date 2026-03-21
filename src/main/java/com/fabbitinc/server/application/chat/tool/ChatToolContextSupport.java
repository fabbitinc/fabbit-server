package com.fabbitinc.server.application.chat.tool;

import com.fabbitinc.server.application.chat.model.ChatExecutionAccumulator;
import java.util.Map;
import java.util.UUID;
import org.springframework.ai.chat.model.ToolContext;

public final class ChatToolContextSupport {

    public static final String RUN_ID = "chatRunId";
    public static final String QUESTION = "chatQuestion";
    public static final String EXECUTION_ACCUMULATOR = "chatExecutionAccumulator";

    private ChatToolContextSupport() {
    }

    public static UUID getRunId(ToolContext toolContext) {
        Object rawValue = getRequired(toolContext, RUN_ID);
        if (rawValue instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(rawValue.toString());
    }

    public static String getQuestion(ToolContext toolContext) {
        Object rawValue = getRequired(toolContext, QUESTION);
        return rawValue == null ? "" : rawValue.toString();
    }

    public static ChatExecutionAccumulator getAccumulator(ToolContext toolContext) {
        Object rawValue = getRequired(toolContext, EXECUTION_ACCUMULATOR);
        if (rawValue instanceof ChatExecutionAccumulator accumulator) {
            return accumulator;
        }
        throw new IllegalStateException("챗 실행 accumulator가 올바르지 않습니다");
    }

    public static Map<String, Object> createContext(UUID runId, String question, ChatExecutionAccumulator accumulator) {
        return Map.of(
                RUN_ID, runId,
                QUESTION, question == null ? "" : question,
                EXECUTION_ACCUMULATOR, accumulator
        );
    }

    private static Object getRequired(ToolContext toolContext, String key) {
        if (toolContext == null) {
            throw new IllegalArgumentException("ToolContext가 필요합니다");
        }
        Object value = toolContext.getContext().get(key);
        if (value == null) {
            throw new IllegalArgumentException("ToolContext에 '" + key + "' 값이 없습니다");
        }
        return value;
    }
}
