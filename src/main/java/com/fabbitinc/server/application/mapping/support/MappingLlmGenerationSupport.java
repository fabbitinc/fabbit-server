package com.fabbitinc.server.application.mapping.support;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.config.AppProperties;
import com.fabbitinc.server.application.mapping.dto.common.MappingResultDto;
import com.fabbitinc.server.application.ontology.support.ManufacturingOntology;
import com.fabbitinc.server.application.ontology.support.OntologyMappingPromptRenderer;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

@Slf4j
@Component
@RequiredArgsConstructor
public class MappingLlmGenerationSupport {

    private static final String SYSTEM_PROMPT_TEMPLATE = "classpath:prompts/mapping/system.st";
    private static final String USER_PROMPT_TEMPLATE = "classpath:prompts/mapping/user.st";
    private static final int MAX_TOKENS = 2000;
    private static final String GPT_5_MINI_MODEL = "openai/gpt-5-mini";
    private static final String MINIMAX_M2_5_MODEL = "minimax/minimax-m2.5";
    private static final String GROK_4_1_FAST_MODEL = "x-ai/grok-4.1-fast";
    private static final String QWEN3_32B_MODEL = "qwen/qwen3-32b";
    private static final String REASONING_EFFORT_LOW = "low";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Map<String, ModelConfig> MODEL_CONFIGS = Map.of(
            GPT_5_MINI_MODEL, new ModelConfig(List.of("openai"), REASONING_EFFORT_LOW),
            MINIMAX_M2_5_MODEL, new ModelConfig(List.of("siliconflow", "friendli"), null),
            GROK_4_1_FAST_MODEL, new ModelConfig(List.of("xai"), null),
            QWEN3_32B_MODEL, new ModelConfig(List.of("deepinfra"), null)
    );

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final MappingNormalizationSupport mappingNormalizationSupport;
    private final ResourceLoader resourceLoader;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    public GenerationOutput generate(List<String> headers, List<Map<String, Object>> sampleRows) {
        requireLlmConfigured();
        return generateByLlm(headers, sampleRows);
    }

    private GenerationOutput generateByLlm(List<String> headers, List<Map<String, Object>> sampleRows)
            throws JacksonException {
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(headers, sampleRows);
        String requestBody = buildRequestBody(systemPrompt, userPrompt);

        long startedAt = System.nanoTime();
        JsonNode responseJson = sendChatCompletionRequest(requestBody);
        String content = extractContent(responseJson);
        String model = extractModel(responseJson);
        int inputTokens = tokenOrZero(responseJson.path("usage").path("prompt_tokens").asInt());
        int outputTokens = tokenOrZero(responseJson.path("usage").path("completion_tokens").asInt());
        double elapsedSeconds = (System.nanoTime() - startedAt) / 1_000_000_000.0;
        if (content.isBlank()) {
            throw new IllegalStateException("llm content is empty");
        }

        String compactJson = stripCodeFence(content);
        JsonNode generatedJson = objectMapper.readTree(compactJson);
        if (generatedJson.isArray()) {
            if (generatedJson.size() == 1 && generatedJson.get(0).isObject()) {
                generatedJson = generatedJson.get(0);
            } else {
                throw new IllegalStateException("llm response array format is invalid");
            }
        }
        if (!generatedJson.isObject()) {
            throw new IllegalStateException("llm response is not json object");
        }

        log.info(
                "event=mapping_preview_llm_completed model={} elapsed={} input_tokens={} output_tokens={}",
                model,
                elapsedSeconds,
                inputTokens,
                outputTokens
        );
        return new GenerationOutput(
                mappingNormalizationSupport.normalize(objectMapper.treeToValue(generatedJson, MappingResultDto.class)),
                model,
                inputTokens,
                outputTokens
        );
    }

    private void requireLlmConfigured() {
        if (!appProperties.llmApiKey().isBlank()) {
            return;
        }
        throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "LLM API 키가 설정되지 않았습니다");
    }

    private JsonNode sendChatCompletionRequest(String requestBody) throws JacksonException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(resolveChatCompletionsUrl()))
                .timeout(Duration.ofSeconds(appProperties.llmTimeoutSeconds()))
                .header("Authorization", "Bearer " + appProperties.llmApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new IllegalStateException("llm request failed", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("llm request interrupted", ex);
        }

        String responseBody = response.body() == null ? "" : response.body();
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("llm request failed: " + extractErrorMessage(responseBody));
        }
        return objectMapper.readTree(responseBody);
    }

    String buildRequestBody(String systemPrompt, String userPrompt) throws JacksonException {
        ObjectNode requestBody = objectMapper.createObjectNode();
        ArrayNode messages = requestBody.putArray("messages");
        messages.addObject()
                .put("role", "system")
                .put("content", systemPrompt);
        messages.addObject()
                .put("role", "user")
                .put("content", userPrompt);
        requestBody.put("model", appProperties.llmModel());
        requestBody.put("temperature", 0);
        requestBody.put("max_tokens", MAX_TOKENS);
        requestBody.putObject("response_format")
                .put("type", "json_object");

        ModelConfig modelConfig = MODEL_CONFIGS.get(appProperties.llmModel());
        if (modelConfig != null) {
            if (modelConfig.reasoningEffort() != null) {
                requestBody.put("reasoning_effort", modelConfig.reasoningEffort());
            }
            if (!modelConfig.providers().isEmpty()) {
                ArrayNode providerOrder = requestBody.putObject("provider")
                        .putArray("order");
                modelConfig.providers().forEach(providerOrder::add);
            }
        }
        return objectMapper.writeValueAsString(requestBody);
    }

    private String extractContent(JsonNode responseJson) {
        JsonNode choices = responseJson.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new IllegalStateException("llm response choices are empty");
        }

        JsonNode content = choices.get(0).path("message").path("content");
        if (content.isTextual()) {
            return content.asText();
        }
        throw new IllegalStateException("llm response content is not text");
    }

    private String extractModel(JsonNode responseJson) {
        String model = responseJson.path("model").asText();
        return model == null || model.isBlank() ? appProperties.llmModel() : model;
    }

    private String extractErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "empty response";
        }
        try {
            JsonNode responseJson = objectMapper.readTree(responseBody);
            String message = responseJson.path("error").path("message").asText();
            if (!message.isBlank()) {
                return message;
            }
        } catch (JacksonException ignored) {
        }
        return responseBody;
    }

    private String resolveChatCompletionsUrl() {
        String baseUrl = normalizeBaseUrl(appProperties.llmBaseUrl());
        if (baseUrl.endsWith("/chat/completions")) {
            return baseUrl;
        }
        if (baseUrl.endsWith("/v1")) {
            return baseUrl + "/chat/completions";
        }
        return baseUrl + "/v1/chat/completions";
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    String buildSystemPrompt() {
        return readPromptTemplate(SYSTEM_PROMPT_TEMPLATE)
                .replace("<ontology_text>", toOntologyPromptText());
    }

    String buildUserPrompt(List<String> headers, List<Map<String, Object>> sampleRows)
            throws JacksonException {
        return readPromptTemplate(USER_PROMPT_TEMPLATE)
                .replace("<headers_json>", formatCompactLikePythonJson(headers))
                .replace("<sample_rows_json>", formatLikePythonJson(sampleRows));
    }

    private String toOntologyPromptText() {
        return OntologyMappingPromptRenderer.render(ManufacturingOntology.ONTOLOGY);
    }

    private String readPromptTemplate(String resourceLocation) {
        Resource resource = resourceLoader.getResource(resourceLocation);
        try {
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "매핑 프롬프트를 읽을 수 없습니다");
        }
    }

    private String formatLikePythonJson(Object value) throws JacksonException {
        JsonNode node = objectMapper.valueToTree(value);
        return formatJsonNode(node, 0);
    }

    private String formatCompactLikePythonJson(Object value) throws JacksonException {
        JsonNode node = objectMapper.valueToTree(value);
        return formatCompactJsonNode(node);
    }

    private String formatJsonNode(JsonNode node, int indentLevel) throws JacksonException {
        if (node == null || node.isNull() || node.isValueNode()) {
            return objectMapper.writeValueAsString(node);
        }

        if (node.isArray()) {
            if (node.isEmpty()) {
                return "[]";
            }

            String indent = " ".repeat(indentLevel);
            String childIndent = " ".repeat(indentLevel + 2);
            StringBuilder builder = new StringBuilder();
            builder.append("[\n");
            for (int index = 0; index < node.size(); index++) {
                builder.append(childIndent)
                        .append(formatJsonNode(node.get(index), indentLevel + 2));
                if (index < node.size() - 1) {
                    builder.append(',');
                }
                builder.append('\n');
            }
            builder.append(indent).append(']');
            return builder.toString();
        }

        String indent = " ".repeat(indentLevel);
        String childIndent = " ".repeat(indentLevel + 2);
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        int index = 0;
        int size = node.size();
        var fields = node.properties().iterator();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            builder.append(childIndent)
                    .append(objectMapper.writeValueAsString(entry.getKey()))
                    .append(": ")
                    .append(formatJsonNode(entry.getValue(), indentLevel + 2));
            if (index < size - 1) {
                builder.append(',');
            }
            builder.append('\n');
            index++;
        }
        builder.append(indent).append('}');
        return builder.toString();
    }

    private String formatCompactJsonNode(JsonNode node) throws JacksonException {
        if (node == null || node.isNull() || node.isValueNode()) {
            return objectMapper.writeValueAsString(node);
        }

        if (node.isArray()) {
            if (node.isEmpty()) {
                return "[]";
            }

            StringBuilder builder = new StringBuilder();
            builder.append('[');
            for (int index = 0; index < node.size(); index++) {
                builder.append(formatCompactJsonNode(node.get(index)));
                if (index < node.size() - 1) {
                    builder.append(", ");
                }
            }
            builder.append(']');
            return builder.toString();
        }

        StringBuilder builder = new StringBuilder();
        builder.append('{');
        int index = 0;
        int size = node.size();
        var fields = node.properties().iterator();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            builder.append(objectMapper.writeValueAsString(entry.getKey()))
                    .append(": ")
                    .append(formatCompactJsonNode(entry.getValue()));
            if (index < size - 1) {
                builder.append(", ");
            }
            index++;
        }
        builder.append('}');
        return builder.toString();
    }

    private String stripCodeFence(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }

        int firstNewline = trimmed.indexOf('\n');
        if (firstNewline < 0) {
            return trimmed;
        }

        String body = trimmed.substring(firstNewline + 1);
        int lastFence = body.lastIndexOf("```");
        if (lastFence >= 0) {
            body = body.substring(0, lastFence);
        }
        return body.trim();
    }

    private int tokenOrZero(Integer value) {
        return value == null || value < 0 ? 0 : value;
    }

    private record ModelConfig(
            List<String> providers,
            String reasoningEffort
    ) {
    }

    public record GenerationOutput(
            MappingResultDto mapping,
            String model,
            int inputTokens,
            int outputTokens
    ) {
    }
}
