package com.fabbitinc.server.presentation.common.web;

public record ApiErrorResponse(
        String code,
        String message
) {
}
