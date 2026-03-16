package com.fabbitinc.server.presentation.notification.controller;
import com.fabbitinc.server.presentation.workitem.dto.response.UserSummaryResponse;
import com.fabbitinc.server.application.workitem.query.result.UserSummaryResult;

import com.fabbitinc.server.application.notification.query.NotificationQuery;
import com.fabbitinc.server.application.notification.query.condition.NotificationListCondition;
import com.fabbitinc.server.application.notification.query.result.NotificationListResult;
import com.fabbitinc.server.application.notification.query.result.UnreadCountResult;
import com.fabbitinc.server.application.notification.usecase.MarkAllNotificationsReadUseCase;
import com.fabbitinc.server.application.notification.usecase.MarkNotificationReadUseCase;
import com.fabbitinc.server.application.notification.usecase.NotificationStreamUseCase;
import com.fabbitinc.server.application.notification.usecase.command.MarkNotificationReadCommand;
import com.fabbitinc.server.application.notification.usecase.result.NotificationStreamResult;
import com.fabbitinc.server.presentation.notification.dto.response.NotificationListResponse;
import com.fabbitinc.server.presentation.notification.dto.response.UnreadCountResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
@Tag(name = "notifications", description = "알림 조회/읽음 처리/SSE API")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "요청 성공"),
        @ApiResponse(responseCode = "204", description = "읽음 처리 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음")
})
public class NotificationController {

    private final NotificationQuery notificationQuery;
    private final MarkNotificationReadUseCase markNotificationReadUseCase;
    private final MarkAllNotificationsReadUseCase markAllNotificationsReadUseCase;
    private final NotificationStreamUseCase notificationStreamUseCase;

    @Operation(
            summary = "GET /api/v1/notifications",
            description = "cursor 기반 페이지네이션으로 알림 목록을 조회합니다"
    )
    @GetMapping
    public NotificationListResponse listNotifications(
            @Parameter(description = "다음 페이지 조회용 커서")
            @RequestParam(value = "cursor", required = false) UUID cursor,
            @Parameter(description = "조회 건수", example = "20")
            @RequestParam(value = "limit", defaultValue = "20")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다") @Max(value = 50, message = "limit은 50 이하여야 합니다") int limit,
            @Parameter(description = "미읽음 알림만 조회할지 여부", example = "false")
            @RequestParam(value = "unread_only", defaultValue = "false") boolean unreadOnly
    ) {
        return toNotificationListResponse(notificationQuery.list(
                new NotificationListCondition(cursor, limit, unreadOnly)
        ));
    }

    @Operation(
            summary = "GET /api/v1/notifications/unread-count",
            description = "현재 사용자의 미읽음 알림 개수를 조회합니다"
    )
    @GetMapping("/unread-count")
    public UnreadCountResponse getUnreadCount() {
        return toUnreadCountResponse(notificationQuery.getUnreadCount());
    }

    @Operation(
            summary = "PUT /api/v1/notifications/{notificationId}/read",
            description = "알림 1건을 읽음 처리합니다"
    )
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Void> readNotification(
            @Parameter(description = "읽음 처리할 알림 ID")
            @PathVariable UUID notificationId
    ) {
        markNotificationReadUseCase.execute(new MarkNotificationReadCommand(notificationId));
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "PUT /api/v1/notifications/read-all",
            description = "현재 사용자의 미읽음 알림을 모두 읽음 처리합니다"
    )
    @PutMapping("/read-all")
    public ResponseEntity<Void> readAllNotifications() {
        markAllNotificationsReadUseCase.execute();
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "GET /api/v1/notifications/stream",
            description = "SSE 스트림으로 새 알림 이벤트를 실시간 수신합니다"
    )
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> stream() {
        NotificationStreamResult result = notificationStreamUseCase.execute();

        StreamingResponseBody body = outputStream -> {
            Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            try {
                writer.write("event: connected\ndata: {}\n\n");
                writer.flush();

                while (!Thread.currentThread().isInterrupted()) {
                    String data = result.queue().poll(30, TimeUnit.SECONDS);
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
                log.debug("event=notification_stream_closed user_id={} reason=write_failed", result.userId(), ex);
            } finally {
                notificationStreamUseCase.disconnect(result);
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .header("X-Accel-Buffering", "no")
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(body);
    }

    private NotificationListResponse toNotificationListResponse(NotificationListResult result) {
        return new NotificationListResponse(
                result.items().stream()
                        .map(item -> new NotificationListResponse.NotificationItemResponse(
                                item.id(),
                                item.type(),
                                item.actorId(),
                                toMentionPayloadResponse(item.payload()),
                                item.readAt(),
                                item.createdAt()
                        ))
                        .toList(),
                result.nextCursor(),
                result.users().entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                java.util.Map.Entry::getKey,
                                entry -> {
                                    NotificationListResult.NotificationUserSummaryResult user = entry.getValue();
                                    return new NotificationListResponse.NotificationUserSummaryResponse(
                                            user.userId(),
                                            user.fullName(),
                                            user.email(),
                                            user.phone(),
                                            user.profileImageUrl()
                                    );
                                }
                        ))
        );
    }

    private NotificationListResponse.MentionPayloadResponse toMentionPayloadResponse(
            NotificationListResult.MentionPayloadResult result
    ) {
        return new NotificationListResponse.MentionPayloadResponse(
                result.projectId(),
                result.sourceIssueId(),
                result.sourceNumber(),
                result.sourceTitle(),
                result.sourceIssueType(),
                result.isComment()
        );
    }

    private UnreadCountResponse toUnreadCountResponse(UnreadCountResult result) {
        return new UnreadCountResponse(result.count());
    }
}
