package com.fabbitinc.server.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

class SecurityAuthenticationEntryPointTest {

    private final SecurityApiErrorResponseWriter errorResponseWriter = new SecurityApiErrorResponseWriter();
    private final SecurityAuthenticationEntryPoint authenticationEntryPoint =
            new SecurityAuthenticationEntryPoint(errorResponseWriter);

    @Test
    void 인증_실패시_UNAUTHENTICATED_포맷으로_응답한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/issues");
        MockHttpServletResponse response = new MockHttpServletResponse();

        authenticationEntryPoint.commence(
                request,
                response,
                new InsufficientAuthenticationException("인증이 필요합니다")
        );

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertEquals(
                "{\"code\":\"UNAUTHENTICATED\",\"message\":\"인증이 필요합니다\"}",
                response.getContentAsString()
        );
    }

    @Test
    void 토큰_파싱_실패가_있으면_해당_에러코드를_응답한다() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/issues");
        request.setAttribute(
                JwtSecurityContextFilter.AUTH_FAILURE_ATTRIBUTE,
                new AppException(ErrorCode.TOKEN_EXPIRED, "access 토큰이 만료되었습니다")
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        authenticationEntryPoint.commence(
                request,
                response,
                new InsufficientAuthenticationException("인증이 필요합니다")
        );

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertEquals(
                "{\"code\":\"TOKEN_EXPIRED\",\"message\":\"access 토큰이 만료되었습니다\"}",
                response.getContentAsString()
        );
    }
}
