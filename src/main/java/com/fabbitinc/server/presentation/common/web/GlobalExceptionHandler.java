package com.fabbitinc.server.presentation.common.web;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiErrorResponse> handleAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        String message = ex.getMessage() == null ? errorCode.defaultMessage() : ex.getMessage();
        if (errorCode.httpStatus() >= 500) {
            log.error("event=app_exception_handled error_code={} message={}", errorCode.name(), message, ex);
        }
        return ResponseEntity
                .status(errorCode.httpStatus())
                .body(new ApiErrorResponse(errorCode.name(), message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        FieldError firstError = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = firstError != null ? firstError.getDefaultMessage() : "입력값이 올바르지 않습니다";
        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.httpStatus())
                .body(new ApiErrorResponse(ErrorCode.VALIDATION_ERROR.name(), message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse("입력값이 올바르지 않습니다");
        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.httpStatus())
                .body(new ApiErrorResponse(ErrorCode.VALIDATION_ERROR.name(), message));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex
    ) {
        return validationError("필수 요청 파라미터가 누락되었습니다: " + ex.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex
    ) {
        return validationError("요청 파라미터 타입이 올바르지 않습니다: " + ex.getName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex
    ) {
        return validationError("요청 본문 형식이 올바르지 않습니다");
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsable(AsyncRequestNotUsableException ex) {
        log.debug("event=async_request_not_usable reason=client_disconnected message={}", ex.getMessage());
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public void handleAsyncRequestTimeout(AsyncRequestTimeoutException ex) {
        log.debug("event=async_request_timeout message={}", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        log.error("event=unexpected_exception_handled", ex);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.httpStatus())
                .body(new ApiErrorResponse(
                        ErrorCode.INTERNAL_SERVER_ERROR.name(),
                        ErrorCode.INTERNAL_SERVER_ERROR.defaultMessage()
                ));
    }

    private ResponseEntity<ApiErrorResponse> validationError(String message) {
        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.httpStatus())
                .body(new ApiErrorResponse(ErrorCode.VALIDATION_ERROR.name(), message));
    }
}
