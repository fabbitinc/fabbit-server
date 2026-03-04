package com.fabbitinc.server.infrastructure.security;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SecurityAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityApiErrorResponseWriter errorResponseWriter;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        ErrorCode errorCode = ErrorCode.UNAUTHENTICATED;
        String message = ErrorCode.UNAUTHENTICATED.defaultMessage();

        Object authFailure = request.getAttribute(JwtSecurityContextFilter.AUTH_FAILURE_ATTRIBUTE);
        if (authFailure instanceof AppException appException && appException.getErrorCode().httpStatus() == 401) {
            errorCode = appException.getErrorCode();
            message = appException.getMessage();
        }

        errorResponseWriter.write(response, errorCode, message);
    }
}
