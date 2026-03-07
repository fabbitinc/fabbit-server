package com.fabbitinc.server.application.mapping.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.config.AppProperties;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class MappingLlmGenerationSupportTest {

    private static final String USER_TEMPLATE = """
            다음 Excel 데이터를 분석하여 매핑하세요.

            ## 컬럼 헤더
            <headers_json>

            ## 샘플 데이터 (처음 5행)
            <sample_rows_json>
            """;

    @Test
    void generate_llm키가없으면_즉시_예외를_던진다() {
        MappingLlmGenerationSupport support = new MappingLlmGenerationSupport(
                new AppProperties(
                        "lvh.me",
                        10,
                        5,
                        60,
                        7,
                        "http://localhost:5173",
                        "localhost",
                        1025,
                        "",
                        "",
                        false,
                        "noreply@fabbit.io",
                        "Fabbit",
                        false,
                        "",
                        "https://challenges.cloudflare.com/turnstile/v0/siteverify",
                        "http://localhost:9000",
                        "minioadmin",
                        "minioadmin",
                        "fabbit",
                        "",
                        "",
                        "https://openrouter.ai/api/v1",
                        "openai/gpt-5-mini",
                        30
                ),
                new ObjectMapper(),
                mock(MappingNormalizationSupport.class),
                mock(ResourceLoader.class)
        );

        AppException exception = assertThrows(
                AppException.class,
                () -> support.generate(List.of("품번"), List.of(Map.of("품번", "A-001")))
        );

        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, exception.getErrorCode());
        assertEquals("LLM API 키가 설정되지 않았습니다", exception.getMessage());
    }

    @Test
    void buildUserPrompt_원본과같은_json포맷을_사용한다() throws Exception {
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        org.springframework.core.io.Resource userResource = mock(org.springframework.core.io.Resource.class);
        when(resourceLoader.getResource("classpath:prompts/mapping/user.st")).thenReturn(userResource);
        when(userResource.exists()).thenReturn(true);
        when(userResource.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(
                USER_TEMPLATE.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        ));

        MappingLlmGenerationSupport support = new MappingLlmGenerationSupport(
                new AppProperties(
                        "lvh.me", 10, 5, 60, 7, "http://localhost:5173",
                        "localhost", 1025, "", "", false, "noreply@fabbit.io", "Fabbit",
                        false, "", "https://challenges.cloudflare.com/turnstile/v0/siteverify",
                        "http://localhost:9000", "minioadmin", "minioadmin", "fabbit", "",
                        "token", "https://openrouter.ai/api/v1", "openai/gpt-5-mini", 30
                ),
                new ObjectMapper(),
                mock(MappingNormalizationSupport.class),
                resourceLoader
        );

        LinkedHashMap<String, Object> sampleRow = new LinkedHashMap<>();
        sampleRow.put("품번", "A-001");
        sampleRow.put("품명", "브라켓");

        String prompt = support.buildUserPrompt(
                List.of("품번", "품명"),
                List.of(sampleRow)
        );

        assertEquals("""
                다음 Excel 데이터를 분석하여 매핑하세요.

                ## 컬럼 헤더
                ["품번", "품명"]

                ## 샘플 데이터 (처음 5행)
                [
                  {
                    "품번": "A-001",
                    "품명": "브라켓"
                  }
                ]
                """, prompt);
    }

    @Test
    void buildSystemPrompt_리소스를_조합해_ontology메타데이터를_주입한다() throws Exception {
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        org.springframework.core.io.Resource systemResource = mock(org.springframework.core.io.Resource.class);

        when(resourceLoader.getResource("classpath:prompts/mapping/system.st")).thenReturn(systemResource);
        when(systemResource.getInputStream()).thenReturn(new java.io.ByteArrayInputStream("""
                시작
                <ontology_text>
                끝
                """.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        MappingLlmGenerationSupport support = new MappingLlmGenerationSupport(
                appProperties("token"),
                new ObjectMapper(),
                mock(MappingNormalizationSupport.class),
                resourceLoader
        );

        String prompt = support.buildSystemPrompt();
        assertTrue(prompt.startsWith("시작\n# Fabbit 제조업 온톨로지 - 매핑 가이드\n"));
        assertTrue(prompt.contains("Part No."));
        assertTrue(prompt.endsWith("끝\n"));
    }

    @Test
    void buildRequestBody_openrouter옵션을_원본과같이_포함한다() throws Exception {
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        org.springframework.core.io.Resource userResource = mock(org.springframework.core.io.Resource.class);
        when(resourceLoader.getResource("classpath:prompts/mapping/user.st")).thenReturn(userResource);
        when(userResource.getInputStream()).thenReturn(new java.io.ByteArrayInputStream(
                USER_TEMPLATE.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        ));

        MappingLlmGenerationSupport support = new MappingLlmGenerationSupport(
                appProperties("token"),
                new ObjectMapper(),
                mock(MappingNormalizationSupport.class),
                resourceLoader
        );

        String body = support.buildRequestBody("system-prompt", "user-prompt");
        JsonNode json = new ObjectMapper().readTree(body);

        assertEquals("openai/gpt-5-mini", json.path("model").asText());
        assertEquals(0, json.path("temperature").asInt());
        assertEquals(2000, json.path("max_tokens").asInt());
        assertEquals("json_object", json.path("response_format").path("type").asText());
        assertEquals("low", json.path("reasoning_effort").asText());
        assertEquals("openai", json.path("provider").path("order").get(0).asText());
        assertEquals("system", json.path("messages").get(0).path("role").asText());
        assertEquals("system-prompt", json.path("messages").get(0).path("content").asText());
        assertEquals("user", json.path("messages").get(1).path("role").asText());
        assertEquals("user-prompt", json.path("messages").get(1).path("content").asText());
    }

    @Test
    void buildSystemPrompt_확장속성대안필드_규칙을_포함한다() {
        MappingLlmGenerationSupport support = new MappingLlmGenerationSupport(
                appProperties("token"),
                new ObjectMapper(),
                mock(MappingNormalizationSupport.class),
                new DefaultResourceLoader()
        );

        String prompt = support.buildSystemPrompt();

        assertTrue(prompt.contains("suggested_extended_property"));
        assertTrue(prompt.contains("_ext_carbon_emission"));
    }

    private AppProperties appProperties(String llmApiKey) {
        return new AppProperties(
                "lvh.me",
                10,
                5,
                60,
                7,
                "http://localhost:5173",
                "localhost",
                1025,
                "",
                "",
                false,
                "noreply@fabbit.io",
                "Fabbit",
                false,
                "",
                "https://challenges.cloudflare.com/turnstile/v0/siteverify",
                "http://localhost:9000",
                "minioadmin",
                "minioadmin",
                "fabbit",
                "",
                llmApiKey,
                "https://openrouter.ai/api/v1",
                "openai/gpt-5-mini",
                30
        );
    }
}
