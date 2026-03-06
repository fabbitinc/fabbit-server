package com.fabbitinc.server.application.mapping.support;

import com.fabbitinc.server.application.config.AppProperties;
import com.fabbitinc.server.application.mapping.dto.common.MappingResultDto;
import com.fabbitinc.server.application.ontology.support.ManufacturingOntology;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MappingLlmGenerationSupport {

    private static final String SYSTEM_PROMPT_TEMPLATE = "classpath:prompts/mapping/system.st";
    private static final String USER_PROMPT_TEMPLATE = "classpath:prompts/mapping/user.st";

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final MappingGenerationSupport mappingGenerationSupport;
    private final ResourceLoader resourceLoader;
    private final StTemplateRenderer templateRenderer = StTemplateRenderer.builder().build();

    public boolean isLlmEnabled() {
        return !appProperties.llmApiKey().isBlank();
    }

    public GenerationOutput generate(List<String> headers, List<Map<String, Object>> sampleRows) {
        if (!isLlmEnabled()) {
            return GenerationOutput.heuristic(mappingGenerationSupport.generate(headers, sampleRows));
        }

        try {
            GenerationOutput generated = generateByLlm(headers, sampleRows);
            if (generated.mapping().propertyMappings().isEmpty() && generated.mapping().relationMappings().isEmpty()) {
                log.warn("event=mapping_preview_llm_empty model={} fallback=heuristic", appProperties.llmModel());
                return generated.withMapping(mappingGenerationSupport.generate(headers, sampleRows));
            }
            return generated;
        } catch (LinkageError | Exception ex) {
            log.warn("event=mapping_preview_llm_failed model={} fallback=heuristic reason={}",
                    appProperties.llmModel(),
                    ex.getMessage());
            return GenerationOutput.heuristic(mappingGenerationSupport.generate(headers, sampleRows));
        }
    }

    private GenerationOutput generateByLlm(List<String> headers, List<Map<String, Object>> sampleRows)
            throws JacksonException {
        String systemPrompt = buildSystemPromptFromTemplate();
        String userPrompt = buildUserPromptFromTemplate(headers, sampleRows);

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(appProperties.llmModel())
                .temperature(0.0)
                .maxTokens(2000)
                .responseFormat(ResponseFormat.builder()
                        .type(ResponseFormat.Type.JSON_OBJECT)
                        .build())
                .build();

        long startedAt = System.nanoTime();
        ResponseEntity<ChatResponse, String> responseEntity = createChatClient(options)
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .options(options)
                .call()
                .responseEntity(String.class);
        String content = responseEntity.entity();
        ChatResponse chatResponse = responseEntity.response();
        ChatResponseMetadata metadata = chatResponse == null ? null : chatResponse.getMetadata();
        Usage usage = metadata == null ? null : metadata.getUsage();
        String model = metadata != null && metadata.getModel() != null && !metadata.getModel().isBlank()
                ? metadata.getModel()
                : appProperties.llmModel();
        int inputTokens = tokenOrZero(usage == null ? null : usage.getPromptTokens());
        int outputTokens = tokenOrZero(usage == null ? null : usage.getCompletionTokens());
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
                objectMapper.treeToValue(generatedJson, MappingResultDto.class),
                true,
                model,
                inputTokens,
                outputTokens
        );
    }

    private ChatClient createChatClient(OpenAiChatOptions options) {
        String baseUrl = normalizeBaseUrl(appProperties.llmBaseUrl());
        String completionsPath = resolveCompletionsPath(baseUrl);

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(appProperties.llmApiKey())
                .completionsPath(completionsPath)
                .build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();

        return ChatClient.builder(chatModel).build();
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    private String resolveCompletionsPath(String baseUrl) {
        if (baseUrl.endsWith("/v1")) {
            return "/chat/completions";
        }
        return "/v1/chat/completions";
    }

    private String buildSystemPromptFromTemplate() {
        return renderPrompt(
                SYSTEM_PROMPT_TEMPLATE,
                Map.of("ontology_text", toOntologyPromptText())
        );
    }

    private String buildUserPromptFromTemplate(List<String> headers, List<Map<String, Object>> sampleRows)
            throws JacksonException {
        return renderPrompt(
                USER_PROMPT_TEMPLATE,
                Map.of(
                        "headers_json", objectMapper.writeValueAsString(headers),
                        "sample_rows_json", objectMapper.writeValueAsString(sampleRows)
                )
        );
    }

    private String renderPrompt(String templateLocation, Map<String, Object> variables) {
        Resource resource = resourceLoader.getResource(templateLocation);
        PromptTemplate template = PromptTemplate.builder()
                .resource(resource)
                .renderer(templateRenderer)
                .build();
        return template.render(variables);
    }

    private String toOntologyPromptText() {
        StringBuilder builder = new StringBuilder();

        builder.append("Node Labels:\n");
        for (ManufacturingOntology.NodeLabelDef node : ManufacturingOntology.ONTOLOGY.nodeLabels()) {
            builder.append("- ").append(node.label())
                    .append(" (merge_keys=").append(node.mergeKeys()).append(")\n");
            for (ManufacturingOntology.PropertyDef property : node.properties()) {
                builder.append("  - ").append(property.name())
                        .append(" : ").append(property.dataType().value())
                        .append(property.required() ? " [required]" : "")
                        .append(property.isMergeKey() ? " [merge_key]" : "")
                        .append('\n');
            }
        }

        builder.append("Relationship Types:\n");
        for (ManufacturingOntology.RelationshipTypeDef relation : ManufacturingOntology.ONTOLOGY.relationshipTypes()) {
            builder.append("- ").append(relation.relType().value())
                    .append(" : ").append(relation.fromLabel())
                    .append(" -> ").append(relation.toLabel())
                    .append('\n');
            for (ManufacturingOntology.PropertyDef property : relation.properties()) {
                builder.append("  - ").append(property.name())
                        .append(" : ").append(property.dataType().value())
                        .append('\n');
            }
        }

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

    public record GenerationOutput(
            MappingResultDto mapping,
            boolean usedLlm,
            String model,
            int inputTokens,
            int outputTokens
    ) {
        public static GenerationOutput heuristic(MappingResultDto mapping) {
            return new GenerationOutput(mapping, false, null, 0, 0);
        }

        public GenerationOutput withMapping(MappingResultDto mapping) {
            return new GenerationOutput(mapping, usedLlm, model, inputTokens, outputTokens);
        }
    }
}
