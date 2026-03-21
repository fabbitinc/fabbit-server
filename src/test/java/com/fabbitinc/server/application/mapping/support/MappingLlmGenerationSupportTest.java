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
            다음 Excel 데이터를 분석하여 V2 매핑을 생성하세요.

            ## 컬럼 헤더
            <headers_json>

            ## 샘플 데이터 (처음 5행)
            <sample_rows_json>
            """;

    @Test
    void generate_llm키가없으면_즉시_예외를_던진다() {
        MappingLlmGenerationSupport support = new MappingLlmGenerationSupport(
                appProperties(""),
                new ObjectMapper(),
                mock(MappingNormalizationSupport.class),
                mock(ResourceLoader.class)
        );

        AppException exception = assertThrows(
                AppException.class,
                () -> support.generate(List.of("품번"), List.of(Map.of("품번", "A-001")))
        );

        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, exception.getErrorCode());
    }

    @Test
    void buildUserPrompt_원본과같은_json포맷을_사용한다() throws Exception {
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

        LinkedHashMap<String, Object> sampleRow = new LinkedHashMap<>();
        sampleRow.put("품번", "A-001");
        sampleRow.put("업체명", "ACME");

        String prompt = support.buildUserPrompt(
                List.of("품번", "업체명"),
                List.of(sampleRow)
        );

        assertEquals("""
                다음 Excel 데이터를 분석하여 V2 매핑을 생성하세요.

                ## 컬럼 헤더
                ["품번", "업체명"]

                ## 샘플 데이터 (처음 5행)
                [
                  {
                    "품번": "A-001",
                    "업체명": "ACME"
                  }
                ]
                """, prompt);
    }

    @Test
    void buildSystemPrompt_v2구조_규칙을_포함한다() {
        MappingLlmGenerationSupport support = new MappingLlmGenerationSupport(
                appProperties("token"),
                new ObjectMapper(),
                mock(MappingNormalizationSupport.class),
                new DefaultResourceLoader()
        );

        String prompt = support.buildSystemPrompt();
        assertTrue(prompt.contains("\"nodes\""));
        assertTrue(prompt.contains("\"relations\""));
        assertTrue(prompt.contains("node_id"));
        assertTrue(prompt.contains("generated_key"));
    }

    @Test
    void buildRequestBody_openrouter옵션을_포함한다() throws Exception {
        MappingLlmGenerationSupport support = new MappingLlmGenerationSupport(
                appProperties("token"),
                new ObjectMapper(),
                mock(MappingNormalizationSupport.class),
                new DefaultResourceLoader()
        );

        String body = support.buildRequestBody("system-prompt", "user-prompt");
        JsonNode json = new ObjectMapper().readTree(body);

        assertEquals("openai/gpt-5-mini", json.path("model").asText());
        assertEquals("json_object", json.path("response_format").path("type").asText());
        assertEquals("system-prompt", json.path("messages").get(0).path("content").asText());
        assertEquals("user-prompt", json.path("messages").get(1).path("content").asText());
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
                true,
                "https://openrouter.ai/api/v1",
                "openai/gpt-5-mini",
                30
        );
    }
}
