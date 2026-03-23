package com.fabbitinc.server.application.chat.support;

import com.fabbitinc.server.application.config.AppProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 사용자 입력에 대한 다계층 가드레일 파이프라인.
 *
 * <pre>
 * Input Guard Pipeline:
 * ├─ 1. format validation       (입력 형식 검증)
 * ├─ 2. rate/length check       (길이 제한)
 * ├─ 3. jailbreak/injection     (정적 패턴 탐지)
 * ├─ 4. pii/security check      (PII 마스킹)
 * ├─ 5. harmful content check   (LLM 기반 유해성 판단)
 * └─ 6. business rule check     (비즈니스 규칙)
 * </pre>
 *
 * 각 단계는 {@link GuardResult}를 반환하며, block이면 즉시 중단합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatInputGuard {

    private static final String GUARD_PROMPT_TEMPLATE = "classpath:prompts/chat/guard.st";
    private static final int MAX_INPUT_LENGTH = 4000;

    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("(?i)ignore\\s+(all\\s+)?previous\\s+instructions?"),
            Pattern.compile("(?i)reveal\\s+(your\\s+)?(system\\s+)?prompt"),
            Pattern.compile("(?i)you\\s+are\\s+now\\s+"),
            Pattern.compile("(?i)developer\\s+mode"),
            Pattern.compile("(?i)DAN\\s+mode"),
            Pattern.compile("(?i)bypass\\s+(all\\s+)?restrictions?"),
            Pattern.compile("(?i)pretend\\s+(you\\s+are|to\\s+be)"),
            Pattern.compile("(?i)act\\s+as\\s+if\\s+you\\s+have\\s+no\\s+restrictions"),
            Pattern.compile("(?i)override\\s+(previous|safety)\\s+"),
            Pattern.compile("이전\\s+지시(사항)?\\s*무시"),
            Pattern.compile("시스템\\s*프롬프트\\s*(공개|알려|보여)"),
            Pattern.compile("(?i)system\\s*prompt")
    );

    private static final Pattern ZERO_WIDTH_CHARS = Pattern.compile("[\\u200b\\u200c\\u200d\\ufeff]");
    private static final double BLOCK_CONFIDENCE_THRESHOLD = 0.9;

    private final AppProperties appProperties;
    private final OpenAiChatModel openAiChatModel;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    /**
     * 파이프라인 순서대로 검사를 실행합니다. block이 발생하면 즉시 반환합니다.
     */
    public GuardResult check(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return GuardResult.allow();
        }

        String normalized = normalize(userInput);

        // 1. format validation
        GuardResult formatResult = checkFormat(normalized);
        if (formatResult.blocked()) {
            return formatResult;
        }

        // 2. rate/length check
        GuardResult lengthResult = checkLength(normalized);
        if (lengthResult.blocked()) {
            return lengthResult;
        }

        // 3. jailbreak/prompt injection (정적 패턴)
        GuardResult injectionResult = checkInjectionPatterns(normalized);
        if (injectionResult.blocked()) {
            return injectionResult;
        }

        // 4. pii/security check
        // TODO: PII 탐지/마스킹 필요 시 구현 (이메일, 전화번호, 카드번호 등)
        // GuardResult piiResult = checkPii(normalized);
        // if (piiResult.blocked()) return piiResult;

        // 5. harmful content check (LLM 기반)
        GuardResult harmfulResult = checkWithLlm(normalized);
        if (harmfulResult.blocked()) {
            return harmfulResult;
        }

        // 6. business rule check
        // TODO: 도메인 특화 규칙 필요 시 구현 (금지 키워드, 권한 검증 등)
        // GuardResult businessResult = checkBusinessRules(normalized);
        // if (businessResult.blocked()) return businessResult;

        return GuardResult.allow();
    }

    // --- 1. format validation ---

    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]");
    private static final Pattern BASE64_BLOCK = Pattern.compile("(?i)base64[,:]\\s*[A-Za-z0-9+/=]{100,}");
    private static final Pattern HEX_ESCAPE_SEQUENCE = Pattern.compile("(?i)(\\\\x[0-9a-f]{2}){4,}");

    private GuardResult checkFormat(String input) {
        if (CONTROL_CHARS.matcher(input).find()) {
            log.warn("event=chat_guard_format_blocked reason=control_chars");
            return GuardResult.block("invalid_format", 1.0);
        }
        if (BASE64_BLOCK.matcher(input).find()) {
            log.warn("event=chat_guard_format_blocked reason=base64_payload");
            return GuardResult.block("encoded_payload", 1.0);
        }
        if (HEX_ESCAPE_SEQUENCE.matcher(input).find()) {
            log.warn("event=chat_guard_format_blocked reason=hex_escape");
            return GuardResult.block("encoded_payload", 1.0);
        }
        return GuardResult.allow();
    }

    // --- 2. rate/length check ---

    private GuardResult checkLength(String input) {
        if (input.length() > MAX_INPUT_LENGTH) {
            log.warn("event=chat_guard_length_blocked length={}", input.length());
            return GuardResult.block("input_too_long", 1.0);
        }
        return GuardResult.allow();
    }

    // --- 3. jailbreak/prompt injection ---

    private GuardResult checkInjectionPatterns(String input) {
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                log.warn("event=chat_guard_injection_blocked pattern={}", pattern.pattern());
                return GuardResult.block("prompt_injection", 1.0);
            }
        }
        return GuardResult.allow();
    }

    // --- 5. harmful content check (LLM) ---

    private static final String GUARD_RESPONSE_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "label": { "type": "string", "enum": ["allow", "block"] },
                "reason_code": { "type": "string" },
                "confidence": { "type": "number" }
              },
              "required": ["label", "reason_code", "confidence"],
              "additionalProperties": false
            }
            """;

    private GuardResult checkWithLlm(String userInput) {
        if (!appProperties.llmEnabled() || appProperties.llmApiKey().isBlank()) {
            return GuardResult.allow();
        }

        try {
            String guardPrompt = readTemplate(GUARD_PROMPT_TEMPLATE)
                    .replace("{{user_input}}", userInput);

            ResponseFormat responseFormat = ResponseFormat.builder()
                    .type(ResponseFormat.Type.JSON_SCHEMA)
                    .jsonSchema(ResponseFormat.JsonSchema.builder()
                            .name("guard_result")
                            .schema(GUARD_RESPONSE_SCHEMA)
                            .strict(true)
                            .build())
                    .build();

            OpenAiChatOptions guardOptions = OpenAiChatOptions.builder()
                    .model(appProperties.llmGuardModel())
                    .temperature(0.0)
                    .maxTokens(50)
                    .responseFormat(responseFormat)
                    .build();

            ChatResponse response = openAiChatModel.call(
                    new Prompt(List.of(new UserMessage(guardPrompt)), guardOptions)
            );

            String content = response.getResult() == null
                    ? ""
                    : response.getResult().getOutput().getText();

            var usage = response.getMetadata() == null ? null : response.getMetadata().getUsage();
            int guardInputTokens = usage == null || usage.getPromptTokens() == null ? 0 : usage.getPromptTokens().intValue();
            int guardOutputTokens = usage == null || usage.getCompletionTokens() == null ? 0 : usage.getCompletionTokens().intValue();

            if (content == null || content.isBlank()) {
                return GuardResult.allow(guardInputTokens, guardOutputTokens);
            }

            JsonNode json = objectMapper.readTree(content);
            String label = json.path("label").asText("allow");
            String reasonCode = json.path("reason_code").asText("unknown");
            double confidence = json.path("confidence").asDouble(0.0);

            if ("block".equals(label) && confidence >= BLOCK_CONFIDENCE_THRESHOLD) {
                log.warn("event=chat_guard_llm_blocked reason_code={} confidence={} input_length={}",
                        reasonCode, confidence, userInput.length());
                return GuardResult.block(reasonCode, confidence, guardInputTokens, guardOutputTokens);
            }

            if ("block".equals(label)) {
                log.info("event=chat_guard_llm_low_confidence reason_code={} confidence={} input_length={}",
                        reasonCode, confidence, userInput.length());
            }

            return GuardResult.allow(guardInputTokens, guardOutputTokens);
        } catch (RuntimeException ex) {
            log.warn("event=chat_guard_llm_error reason={}", ex.getMessage());
            return GuardResult.allow();
        }
    }

    // --- 공통 유틸 ---

    private String normalize(String input) {
        return ZERO_WIDTH_CHARS.matcher(input).replaceAll("").strip();
    }

    private String readTemplate(String resourceLocation) {
        try {
            return StreamUtils.copyToString(
                    resourceLoader.getResource(resourceLocation).getInputStream(),
                    StandardCharsets.UTF_8
            );
        } catch (IOException ex) {
            log.warn("event=chat_guard_template_read_error resource={}", resourceLocation);
            return "다음 메시지를 분류하세요. JSON으로 응답: {\"label\": \"allow\" 또는 \"block\"}\n\n{{user_input}}";
        }
    }

    /**
     * 가드레일 판정 결과.
     *
     * @param label      allow | block | review
     * @param reasonCode 판정 사유 (prompt_injection, llm_harmful_detected, input_too_long 등)
     * @param confidence 판정 신뢰도 (0.0 ~ 1.0). 정적 필터는 1.0, LLM 판단은 모델 신뢰도.
     */
    public record GuardResult(
            String label,
            String reasonCode,
            double confidence,
            int inputTokens,
            int outputTokens
    ) {
        public boolean blocked() {
            return "block".equals(label);
        }

        public static GuardResult allow() {
            return new GuardResult("allow", "benign", 1.0, 0, 0);
        }

        public static GuardResult allow(int inputTokens, int outputTokens) {
            return new GuardResult("allow", "benign", 1.0, inputTokens, outputTokens);
        }

        public static GuardResult block(String reasonCode, double confidence) {
            return new GuardResult("block", reasonCode, confidence, 0, 0);
        }

        public static GuardResult block(String reasonCode, double confidence, int inputTokens, int outputTokens) {
            return new GuardResult("block", reasonCode, confidence, inputTokens, outputTokens);
        }

        // TODO: review 판정이 필요할 때 활성화 (사람 검토 큐로 전달)
        // public static GuardResult review(String reasonCode, double confidence) {
        //     return new GuardResult("review", reasonCode, confidence, 0, 0);
        // }
    }
}
