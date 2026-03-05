package com.fabbitinc.server.presentation.common.web;

import com.fabbitinc.server.application.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.core.MethodParameter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Method;

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

    @Test
    void 파라미터_타입_변환_실패는_validation_error로_응답한다() throws NoSuchMethodException {
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("dummy", int.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "NaN",
                Integer.class,
                "issueNumber",
                methodParameter,
                new NumberFormatException("For input string: \"NaN\"")
        );

        ResponseEntity<ApiErrorResponse> response =
                globalExceptionHandler.handleMethodArgumentTypeMismatch(exception);

        assertEquals(ErrorCode.VALIDATION_ERROR.httpStatus(), response.getStatusCode().value());
        assertEquals(ErrorCode.VALIDATION_ERROR.name(), response.getBody().code());
        assertTrue(response.getBody().message().contains("issueNumber"));
    }

    @Test
    void 요청본문_역직렬화_실패는_validation_error로_응답한다() {
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "JSON parse error",
                new MockHttpInputMessage(new byte[0])
        );

        ResponseEntity<ApiErrorResponse> response =
                globalExceptionHandler.handleHttpMessageNotReadable(exception);

        assertEquals(ErrorCode.VALIDATION_ERROR.httpStatus(), response.getStatusCode().value());
        assertEquals(ErrorCode.VALIDATION_ERROR.name(), response.getBody().code());
        assertEquals("요청 본문 형식이 올바르지 않습니다", response.getBody().message());
    }

    private void dummy(int issueNumber) {
        // MethodParameter 생성을 위한 테스트 전용 메서드
    }
}
