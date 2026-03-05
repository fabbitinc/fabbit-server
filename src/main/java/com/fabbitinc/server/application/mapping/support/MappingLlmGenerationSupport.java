package com.fabbitinc.server.application.mapping.support;

import com.fabbitinc.server.application.config.AppProperties;
import com.fabbitinc.server.application.mapping.dto.common.MappingResultDto;
import com.fabbitinc.server.application.ontology.support.ManufacturingOntology;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.ResponseFormat;
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

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final MappingGenerationSupport mappingGenerationSupport;

    public MappingResultDto generate(List<String> headers, List<Map<String, Object>> sampleRows) {
        if (appProperties.llmApiKey().isBlank()) {
            return mappingGenerationSupport.generate(headers, sampleRows);
        }

        try {
            MappingResultDto generated = generateByLlm(headers, sampleRows);
            if (generated.propertyMappings().isEmpty() && generated.relationMappings().isEmpty()) {
                log.warn("event=mapping_preview_llm_empty model={} fallback=heuristic", appProperties.llmModel());
                return mappingGenerationSupport.generate(headers, sampleRows);
            }
            return generated;
        } catch (Exception ex) {
            log.warn("event=mapping_preview_llm_failed model={} fallback=heuristic reason={}",
                    appProperties.llmModel(),
                    ex.getMessage());
            return mappingGenerationSupport.generate(headers, sampleRows);
        }
    }

    private MappingResultDto generateByLlm(List<String> headers, List<Map<String, Object>> sampleRows)
            throws JacksonException {
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(headers, sampleRows);

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(appProperties.llmModel())
                .temperature(0.0)
                .maxTokens(2000)
                .responseFormat(ResponseFormat.builder()
                        .type(ResponseFormat.Type.JSON_OBJECT)
                        .build())
                .build();

        long startedAt = System.nanoTime();
        String content = createChatClient(options)
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .options(options)
                .call()
                .content();
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

        log.info("event=mapping_preview_llm_completed model={} elapsed={}", appProperties.llmModel(), elapsedSeconds);
        return objectMapper.treeToValue(generatedJson, MappingResultDto.class);
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

    private String buildUserPrompt(List<String> headers, List<Map<String, Object>> sampleRows) throws JacksonException {
        return "다음 Excel 데이터를 분석하여 매핑하세요.\n\n"
                + "## 컬럼 헤더\n"
                + objectMapper.writeValueAsString(headers)
                + "\n\n## 샘플 데이터 (최대 5행)\n"
                + objectMapper.writeValueAsString(sampleRows);
    }

    private String buildSystemPrompt() {
        StringBuilder builder = new StringBuilder();
        builder.append("당신은 제조업 데이터 매핑 전문가입니다.\n")
                .append("Excel 헤더와 샘플 데이터를 분석해 제조 온톨로지 매핑 JSON을 만드세요.\n\n")
                .append("## 온톨로지\n")
                .append(toOntologyPromptText())
                .append("\n\n## 출력 규칙\n")
                .append("- JSON object 하나만 출력하세요.\n")
                .append("- 키는 property_mappings, relation_mappings 두 개를 포함하세요.\n")
                .append("- property_mappings: source_column, target_property, data_type, confidence, reason, is_extended\n")
                .append("- relation_mappings: rel_type, target_label, node_columns, rel_columns, rel_column_types, confidence, reason\n")
                .append("- relation 속성(quantity, unit_cost 등)은 rel_columns로만 매핑하세요.\n")
                .append("- rootless relation 허용: node_columns는 비우고 rel_columns만 채울 수 있습니다.\n")
                .append("- 온톨로지에 없는 Part 속성은 _ext_ 접두사의 snake_case로 변환하세요.\n")
                .append("- 설명, markdown, 코드블록 없이 JSON만 반환하세요.");
        return builder.toString();
    }

    private String toOntologyPromptText() {
        StringBuilder builder = new StringBuilder();

        builder.append("Node Labels:\n");
        for (ManufacturingOntology.NodeLabelDef node : ManufacturingOntology.ONTOLOGY.nodeLabels()) {
            builder.append("- ").append(node.label())
                    .append(" (merge_keys=").append(node.mergeKeys()).append(")\n");
            for (ManufacturingOntology.PropertyDef property : node.properties()) {
                builder.append("  - ").append(property.name())
                        .append(" : ").append(property.dataType())
                        .append(property.required() ? " [required]" : "")
                        .append(property.isMergeKey() ? " [merge_key]" : "")
                        .append('\n');
            }
        }

        builder.append("Relationship Types:\n");
        for (ManufacturingOntology.RelationshipTypeDef relation : ManufacturingOntology.ONTOLOGY.relationshipTypes()) {
            builder.append("- ").append(relation.relType())
                    .append(" : ").append(relation.fromLabel())
                    .append(" -> ").append(relation.toLabel())
                    .append('\n');
            for (ManufacturingOntology.PropertyDef property : relation.properties()) {
                builder.append("  - ").append(property.name())
                        .append(" : ").append(property.dataType())
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
}
