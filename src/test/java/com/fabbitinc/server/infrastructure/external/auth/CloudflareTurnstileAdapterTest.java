package com.fabbitinc.server.infrastructure.external.auth;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.config.AppProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CloudflareTurnstileAdapterTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void turnstile이_비활성화면_검증을_건너뛴다() {
        AppProperties properties = appProperties(false, "", "http://127.0.0.1:9/siteverify");
        CloudflareTurnstileAdapter adapter = new CloudflareTurnstileAdapter(properties, new ObjectMapper());

        assertDoesNotThrow(() -> adapter.verify(null));
    }

    @Test
    void 활성화인데_토큰이_없으면_BAD_REQUEST를_던진다() {
        AppProperties properties = appProperties(true, "secret", "http://127.0.0.1:9/siteverify");
        CloudflareTurnstileAdapter adapter = new CloudflareTurnstileAdapter(properties, new ObjectMapper());

        AppException ex = assertThrows(AppException.class, () -> adapter.verify(" "));

        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
    }

    @Test
    void 활성화인데_시크릿이_없으면_INTERNAL_SERVER_ERROR를_던진다() {
        AppProperties properties = appProperties(true, "", "http://127.0.0.1:9/siteverify");
        CloudflareTurnstileAdapter adapter = new CloudflareTurnstileAdapter(properties, new ObjectMapper());

        AppException ex = assertThrows(AppException.class, () -> adapter.verify("token"));

        assertEquals(ErrorCode.INTERNAL_SERVER_ERROR, ex.getErrorCode());
    }

    @Test
    void siteverify_success_true면_성공한다() throws IOException {
        server = startServer(200, "{\"success\":true}");
        String verifyUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/siteverify";
        AppProperties properties = appProperties(true, "secret", verifyUrl);
        CloudflareTurnstileAdapter adapter = new CloudflareTurnstileAdapter(properties, new ObjectMapper());

        assertDoesNotThrow(() -> adapter.verify("token"));
    }

    @Test
    void siteverify_success_false면_BAD_REQUEST를_던진다() throws IOException {
        server = startServer(200, "{\"success\":false,\"error-codes\":[\"invalid-input-response\"]}");
        String verifyUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/siteverify";
        AppProperties properties = appProperties(true, "secret", verifyUrl);
        CloudflareTurnstileAdapter adapter = new CloudflareTurnstileAdapter(properties, new ObjectMapper());

        AppException ex = assertThrows(AppException.class, () -> adapter.verify("token"));

        assertEquals(ErrorCode.BAD_REQUEST, ex.getErrorCode());
    }

    private HttpServer startServer(int status, String body) throws IOException {
        HttpServer localServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        localServer.createContext("/siteverify", new FixedResponseHandler(status, body));
        localServer.start();
        return localServer;
    }

    private AppProperties appProperties(boolean turnstileEnabled, String turnstileSecretKey, String turnstileVerifyUrl) {
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
                turnstileEnabled,
                turnstileSecretKey,
                turnstileVerifyUrl,
                "http://localhost:9000",
                "minioadmin",
                "minioadmin",
                "fabbit",
                "",
                "",
                "https://openrouter.ai/api/v1",
                "openai/gpt-5-mini",
                30
        );
    }

    private record FixedResponseHandler(int status, String body) implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] payload = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
        }
    }
}
