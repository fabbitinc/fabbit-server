package com.fabbitinc.server.presentation.common.web;

import com.fabbitinc.server.application.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    void 필수_쿼리파라미터_누락은_validation_error로_응답한다() {
        MissingServletRequestParameterException exception =
                new MissingServletRequestParameterException("email", "String");

        ResponseEntity<ApiErrorResponse> response =
                globalExceptionHandler.handleMissingServletRequestParameter(exception);

        assertEquals(ErrorCode.VALIDATION_ERROR.httpStatus(), response.getStatusCode().value());
        assertEquals(ErrorCode.VALIDATION_ERROR.name(), response.getBody().code());
        assertTrue(response.getBody().message().contains("email"));
    }
}
