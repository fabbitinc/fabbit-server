package com.fabbitinc.server.presentation.common.web;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiErrorResponse> handleAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        String message = ex.getMessage() == null ? errorCode.defaultMessage() : ex.getMessage();
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.httpStatus())
                .body(new ApiErrorResponse(
                        ErrorCode.INTERNAL_SERVER_ERROR.name(),
                        ErrorCode.INTERNAL_SERVER_ERROR.defaultMessage()
                ));
    }
}
