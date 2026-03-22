package com.fabbitinc.server.application.chat.support;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

@Component
@RequiredArgsConstructor
public class ChatSystemPromptFactory {

    private static final String SYSTEM_TEMPLATE = "classpath:prompts/chat/system.st";
    private static final String TOOL_FLOW_TEMPLATE = "classpath:prompts/chat/tool-flow.st";

    private final ToolCallbackProvider chatToolCallbackProvider;
    private final ResourceLoader resourceLoader;

    public String create() {
        String toolDescriptions = buildToolDescriptions();
        String toolFlow = readTemplate(TOOL_FLOW_TEMPLATE)
                .replace("{{tool_descriptions}}", toolDescriptions);

        return readTemplate(SYSTEM_TEMPLATE)
                .replace("{{tool_flow}}", toolFlow);
    }

    private String buildToolDescriptions() {
        ToolCallback[] callbacks = chatToolCallbackProvider.getToolCallbacks();
        return Arrays.stream(callbacks)
                .sorted(Comparator.comparing(callback -> callback.getToolDefinition().name()))
                .map(this::formatToolDescription)
                .collect(Collectors.joining("\n"));
    }

    private String formatToolDescription(ToolCallback callback) {
        return "- " + callback.getToolDefinition().name() + ": " + callback.getToolDefinition().description();
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
}
