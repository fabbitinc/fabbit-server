package com.fabbitinc.server.infrastructure.external.auth;

import com.fabbitinc.server.application.auth.port.TurnstilePort;
import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.config.AppProperties;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class CloudflareTurnstileAdapter implements TurnstilePort {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();

    @Override
    public void verify(String token) {
        if (!appProperties.turnstileEnabled()) {
            return;
        }

        if (token == null || token.isBlank()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Turnstile 토큰이 필요합니다");
        }
        if (appProperties.turnstileSecretKey().isBlank()) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Turnstile 비밀키가 설정되지 않았습니다");
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(appProperties.turnstileVerifyUrl()))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(buildFormBody(token)))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException ex) {
            throw new AppException(ErrorCode.BAD_REQUEST, "봇 방지 검증에 실패했습니다");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AppException(ErrorCode.BAD_REQUEST, "봇 방지 검증에 실패했습니다");
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.warn("event=turnstile_http_failed status={}", response.statusCode());
            throw new AppException(ErrorCode.BAD_REQUEST, "봇 방지 검증에 실패했습니다");
        }

        JsonNode result;
        try {
            result = objectMapper.readTree(response.body());
        } catch (JacksonException ex) {
            throw new AppException(ErrorCode.BAD_REQUEST, "봇 방지 검증에 실패했습니다");
        }

        if (!result.path("success").asBoolean(false)) {
            log.warn("event=turnstile_verify_failed error_codes={}", result.path("error-codes"));
            throw new AppException(ErrorCode.BAD_REQUEST, "봇 방지 검증에 실패했습니다");
        }
    }

    private String buildFormBody(String token) {
        return "secret=" + urlEncode(appProperties.turnstileSecretKey())
                + "&response=" + urlEncode(token);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
