package com.fabbitinc.server.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

class SecurityAccessDeniedHandlerTest {

    private final SecurityApiErrorResponseWriter errorResponseWriter = new SecurityApiErrorResponseWriter();
    private final SecurityAccessDeniedHandler accessDeniedHandler =
            new SecurityAccessDeniedHandler(errorResponseWriter);

    @Test
    void 권한_거부시_FORBIDDEN_포맷으로_응답한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/issues");
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(request, response, new AccessDeniedException("권한 없음"));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertEquals(
                "{\"code\":\"FORBIDDEN\",\"message\":\"권한이 없습니다\"}",
                response.getContentAsString()
        );
    }
}
