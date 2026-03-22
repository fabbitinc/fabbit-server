package com.fabbitinc.server.application.chat.support;

public final class ChatEventTypes {

    public static final String RUN_STARTED = "run.started";
    public static final String RUN_COMPLETED = "run.completed";
    public static final String RUN_FAILED = "run.failed";
    public static final String RUN_WAITING_CONFIRMATION = "run.waiting_confirmation";
    public static final String TRACE_UPDATED = "trace.updated";
    public static final String TOOL_STARTED = "tool.started";
    public static final String TOOL_COMPLETED = "tool.completed";
    public static final String TOOL_FAILED = "tool.failed";
    public static final String ACTION_REQUIRED = "action.required";
    public static final String ACTION_COMPLETED = "action.completed";
    public static final String ACTION_REJECTED = "action.rejected";
    public static final String MESSAGE_COMPLETED = "message.completed";

    private ChatEventTypes() {
    }
}
