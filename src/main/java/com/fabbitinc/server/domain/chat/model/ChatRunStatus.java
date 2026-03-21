package com.fabbitinc.server.domain.chat.model;

public enum ChatRunStatus {
    QUEUED,
    RUNNING,
    WAITING_CONFIRMATION,
    COMPLETED,
    FAILED,
    CANCELLED
}
