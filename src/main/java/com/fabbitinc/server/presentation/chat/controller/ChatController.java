package com.fabbitinc.server.presentation.chat.controller;

import com.fabbitinc.server.application.chat.query.ChatQuery;
import com.fabbitinc.server.application.chat.query.condition.ChatMessageListCondition;
import com.fabbitinc.server.application.chat.query.condition.ChatRunEventListCondition;
import com.fabbitinc.server.application.chat.query.condition.ChatThreadDetailCondition;
import com.fabbitinc.server.application.chat.query.condition.ChatThreadListCondition;
import com.fabbitinc.server.application.chat.query.result.ChatMessageListResult;
import com.fabbitinc.server.application.chat.query.result.ChatRunEventListResult;
import com.fabbitinc.server.application.chat.query.result.ChatThreadDetailResult;
import com.fabbitinc.server.application.chat.query.result.ChatThreadListResult;
import com.fabbitinc.server.application.chat.support.ChatMessageComposer;
import com.fabbitinc.server.application.chat.usecase.ConnectChatRunStreamUseCase;
import com.fabbitinc.server.application.chat.usecase.ConfirmChatActionUseCase;
import com.fabbitinc.server.application.chat.usecase.CreateChatThreadUseCase;
import com.fabbitinc.server.application.chat.usecase.RejectChatActionUseCase;
import com.fabbitinc.server.application.chat.usecase.SendChatMessageUseCase;
import com.fabbitinc.server.application.chat.usecase.result.ChatRunStreamResult;
import com.fabbitinc.server.presentation.chat.dto.request.CreateChatThreadRequest;
import com.fabbitinc.server.presentation.chat.dto.request.SendChatMessageRequest;
import com.fabbitinc.server.presentation.chat.dto.response.ChatMessageListResponse;
import com.fabbitinc.server.presentation.chat.dto.response.ChatRunEventListResponse;
import com.fabbitinc.server.presentation.chat.dto.response.ChatThreadDetailResponse;
import com.fabbitinc.server.presentation.chat.dto.response.ChatThreadListResponse;
import com.fabbitinc.server.presentation.chat.dto.response.ConfirmChatActionResponse;
import com.fabbitinc.server.presentation.chat.dto.response.CreateChatThreadResponse;
import com.fabbitinc.server.presentation.chat.dto.response.SendChatMessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat")
@Tag(name = "chat", description = "LLM 챗 스레드/메시지/스트림 API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "201", description = "생성 성공"),
        @ApiResponse(responseCode = "202", description = "비동기 실행 시작"),
        @ApiResponse(responseCode = "204", description = "요청 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
public class ChatController {

    private final ChatQuery chatQuery;
    private final CreateChatThreadUseCase createChatThreadUseCase;
    private final SendChatMessageUseCase sendChatMessageUseCase;
    private final ConnectChatRunStreamUseCase connectChatRunStreamUseCase;
    private final ConfirmChatActionUseCase confirmChatActionUseCase;
    private final RejectChatActionUseCase rejectChatActionUseCase;
    private final ChatMessageComposer chatMessageComposer;

    @Operation(summary = "챗 스레드를 생성합니다", description = "새로운 챗 스레드를 생성합니다")
    @PostMapping("/threads")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateChatThreadResponse createThread(
            @Valid @RequestBody CreateChatThreadRequest request
    ) {
        CreateChatThreadUseCase.CreateChatThreadResult result = createChatThreadUseCase.execute(
                new CreateChatThreadUseCase.CreateChatThreadCommand(
                        request.projectId(),
                        request.contextType(),
                        request.contextId(),
                        request.title()
                )
        );
        return new CreateChatThreadResponse(result.threadId());
    }

    @Operation(summary = "챗 스레드 목록을 조회합니다", description = "현재 사용자의 챗 스레드 목록을 조회합니다")
    @GetMapping("/threads")
    public ChatThreadListResponse listThreads() {
        return toChatThreadListResponse(chatQuery.list(new ChatThreadListCondition()));
    }

    @Operation(summary = "챗 스레드 상세를 조회합니다", description = "챗 스레드 메타데이터를 조회합니다")
    @GetMapping("/threads/{threadId}")
    public ChatThreadDetailResponse getThread(
            @Parameter(description = "조회할 스레드 ID")
            @PathVariable UUID threadId
    ) {
        return toChatThreadDetailResponse(chatQuery.get(new ChatThreadDetailCondition(threadId)));
    }

    @Operation(summary = "챗 메시지 목록을 조회합니다", description = "스레드에 속한 메시지 목록을 조회합니다")
    @GetMapping("/threads/{threadId}/messages")
    public ChatMessageListResponse listMessages(
            @Parameter(description = "조회할 스레드 ID")
            @PathVariable UUID threadId
    ) {
        return toChatMessageListResponse(chatQuery.list(new ChatMessageListCondition(threadId)));
    }

    @Operation(summary = "챗 실행 이벤트 목록을 조회합니다", description = "실행에 속한 단계 이벤트 목록을 조회합니다")
    @GetMapping("/runs/{runId}/events")
    public ChatRunEventListResponse listRunEvents(
            @Parameter(description = "조회할 실행 ID")
            @PathVariable UUID runId
    ) {
        return toChatRunEventListResponse(chatQuery.list(new ChatRunEventListCondition(runId)));
    }

    @Operation(summary = "챗 메시지를 전송합니다", description = "사용자 메시지를 저장하고 비동기 챗 실행을 시작합니다")
    @PostMapping("/threads/{threadId}/messages")
    public ResponseEntity<SendChatMessageResponse> sendMessage(
            @Parameter(description = "메시지를 추가할 스레드 ID")
            @PathVariable UUID threadId,
            @Valid @RequestBody SendChatMessageRequest request
    ) {
        SendChatMessageUseCase.SendChatMessageResult result = sendChatMessageUseCase.execute(
                new SendChatMessageUseCase.SendChatMessageCommand(threadId, request.text())
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new SendChatMessageResponse(result.messageId(), result.runId(), result.status()));
    }

    @Operation(summary = "챗 실행 스트림을 구독합니다", description = "실행 이벤트와 응답을 SSE 스트림으로 수신합니다")
    @GetMapping(value = "/runs/{runId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> streamRun(
            @Parameter(description = "스트림으로 구독할 실행 ID")
            @PathVariable UUID runId,
            @Parameter(description = "이 sequence 이후 이벤트만 재생합니다")
            @RequestParam(value = "last_event_sequence", required = false) Long lastEventSequence,
            @Parameter(description = "SSE 재연결 시 마지막으로 수신한 이벤트 ID입니다")
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId
    ) {
        ChatRunStreamResult result = connectChatRunStreamUseCase.execute(
                runId,
                resolveReplayCursor(lastEventSequence, lastEventId)
        );
        StreamingResponseBody body = outputStream -> {
            Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            try {
                writer.write("event: connected\ndata: {}\n\n");
                writer.flush();

                while (!Thread.currentThread().isInterrupted()) {
                    String data = result.queue().poll(15, TimeUnit.SECONDS);
                    if (data == null) {
                        writer.write(": keepalive\n\n");
                    } else {
                        writer.write(data);
                    }
                    writer.flush();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (IOException | RuntimeException ex) {
                log.debug("event=chat_stream_closed run_id={} reason=write_failed", runId, ex);
            } finally {
                connectChatRunStreamUseCase.disconnect(result);
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .header("X-Accel-Buffering", "no")
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(body);
    }

    @Operation(summary = "챗 액션 요청을 확인합니다", description = "사용자 확인이 필요한 액션 요청을 실제로 실행합니다")
    @PostMapping("/action-requests/{actionRequestId}/confirm")
    public ConfirmChatActionResponse confirmAction(
            @Parameter(description = "확인할 액션 요청 ID")
            @PathVariable UUID actionRequestId
    ) {
        ConfirmChatActionUseCase.ConfirmChatActionResult result = confirmChatActionUseCase.execute(
                new ConfirmChatActionUseCase.ConfirmChatActionCommand(actionRequestId)
        );
        return new ConfirmChatActionResponse(result.actionRequestId(), result.status(), result.issueId());
    }

    @Operation(summary = "챗 액션 요청을 거절합니다", description = "사용자 확인이 필요한 액션 요청을 취소합니다")
    @PostMapping("/action-requests/{actionRequestId}/reject")
    public ResponseEntity<Void> rejectAction(
            @Parameter(description = "거절할 액션 요청 ID")
            @PathVariable UUID actionRequestId
    ) {
        rejectChatActionUseCase.execute(new RejectChatActionUseCase.RejectChatActionCommand(actionRequestId));
        return ResponseEntity.noContent().build();
    }

    private ChatThreadListResponse toChatThreadListResponse(ChatThreadListResult result) {
        return new ChatThreadListResponse(
                result.items().stream()
                        .map(item -> new ChatThreadListResponse.ChatThreadItemResponse(
                                item.threadId(),
                                item.projectId(),
                                item.contextType(),
                                item.contextId(),
                                item.title(),
                                item.status(),
                                item.lastMessageAt(),
                                item.createdAt()
                        ))
                        .toList()
        );
    }

    private ChatThreadDetailResponse toChatThreadDetailResponse(ChatThreadDetailResult result) {
        return new ChatThreadDetailResponse(
                result.threadId(),
                result.projectId(),
                result.contextType(),
                result.contextId(),
                result.title(),
                result.status(),
                result.lastMessageAt(),
                result.createdAt()
        );
    }

    private ChatMessageListResponse toChatMessageListResponse(ChatMessageListResult result) {
        return new ChatMessageListResponse(
                result.items().stream()
                        .map(item -> new ChatMessageListResponse.ChatMessageResponse(
                                item.messageId(),
                                item.runId(),
                                item.role(),
                                item.messageType(),
                                item.status(),
                                item.sequence(),
                                chatMessageComposer.parse(item.content()),
                                item.createdAt()
                        ))
                        .toList()
        );
    }

    private ChatRunEventListResponse toChatRunEventListResponse(ChatRunEventListResult result) {
        return new ChatRunEventListResponse(
                result.items().stream()
                        .map(item -> new ChatRunEventListResponse.ChatRunEventResponse(
                                item.eventId(),
                                item.runId(),
                                item.sequence(),
                                item.eventType(),
                                item.visibility(),
                                chatMessageComposer.parse(item.payload()),
                                item.createdAt()
                        ))
                        .toList()
        );
    }

    private Long resolveReplayCursor(Long lastEventSequence, String lastEventId) {
        if (lastEventSequence != null) {
            return lastEventSequence;
        }
        if (lastEventId == null || lastEventId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(lastEventId.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
