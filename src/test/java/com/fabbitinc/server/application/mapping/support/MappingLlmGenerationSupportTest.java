package com.fabbitinc.server.application.mapping.support;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ResourceLoader;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class MappingLlmGenerationSupportTest {

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
                mock(ResourceLoader.class)
        );

        AppException exception = assertThrows(
                AppException.class,
                () -> support.generate(List.of("품번"), List.of(Map.of("품번", "A-001")))
        );

        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, exception.getErrorCode());
        assertEquals("LLM API 키가 설정되지 않았습니다", exception.getMessage());
    }
}
