package com.fabbitinc.server.application.common.exception;

public enum ErrorCode {
    UNAUTHENTICATED(401, "인증이 필요합니다"),
    INVALID_CREDENTIALS(401, "인증 정보가 올바르지 않습니다"),
    TOKEN_EXPIRED(401, "토큰이 만료되었습니다"),
    TOKEN_INVALID(401, "유효하지 않은 토큰입니다"),
    FORBIDDEN(403, "권한이 없습니다"),
    PART_WORKFLOW_POLICY_FORBIDDEN(403, "현재 부품 워크플로 정책상 허용되지 않는 작업입니다"),
    BAD_REQUEST(400, "잘못된 요청입니다"),

    NOT_FOUND(404, "리소스를 찾을 수 없습니다"),
    ALREADY_EXISTS(409, "이미 존재하는 리소스입니다"),
    CONFLICT(409, "리소스 충돌이 발생했습니다"),
    VALIDATION_ERROR(422, "입력값이 올바르지 않습니다"),
    RATE_LIMITED(429, "요청이 너무 많습니다"),
    INVALID_CODE(400, "인증코드가 올바르지 않습니다"),
    MAX_ATTEMPTS_EXCEEDED(400, "인증 시도 횟수를 초과했습니다"),
    CODE_EXPIRED(400, "인증코드가 만료되었습니다"),
    INVALID_VERIFICATION(400, "유효하지 않은 인증 정보입니다"),
    PRECONDITION_FAILED(412, "요청 선행 조건을 만족하지 않습니다"),
    INVALID_STATE(409, "리소스 상태가 올바르지 않습니다"),
    PROJECT_ARCHIVED(409, "보관된 프로젝트는 수정할 수 없습니다"),

    QUOTA_EXCEEDED(402, "사용량 한도를 초과했습니다"),
    MEMBER_LIMIT_EXCEEDED(402, "멤버 수 한도를 초과했습니다"),
    SUBSCRIPTION_NOT_FOUND(402, "구독 정보를 찾을 수 없습니다"),

    INTERNAL_SERVER_ERROR(500, "서버 내부 오류가 발생했습니다");

    private final int httpStatus;
    private final String defaultMessage;

    ErrorCode(int httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
