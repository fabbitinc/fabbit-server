package com.fabbitinc.server.presentation.notification.controller;

import com.fabbitinc.server.application.notification.dto.response.NotificationListResponse;
import com.fabbitinc.server.application.notification.dto.response.UnreadCountResponse;
import com.fabbitinc.server.application.notification.query.NotificationQuery;
import com.fabbitinc.server.application.notification.usecase.MarkAllNotificationsReadUseCase;
import com.fabbitinc.server.application.notification.usecase.MarkNotificationReadUseCase;
import com.fabbitinc.server.application.notification.usecase.NotificationStreamSession;
import com.fabbitinc.server.application.notification.usecase.NotificationStreamUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
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

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
@Tag(name = "notifications", description = "알림 조회/읽음 처리/SSE API")
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
            @RequestParam(value = "cursor", required = false) UUID cursor,
            @RequestParam(value = "limit", defaultValue = "20")
            @Min(value = 1, message = "limit은 1 이상이어야 합니다")
            @Max(value = 50, message = "limit은 50 이하여야 합니다")
            int limit,
            @RequestParam(value = "unread_only", defaultValue = "false") boolean unreadOnly
    ) {
        return notificationQuery.listNotifications(cursor, limit, unreadOnly);
    }

    @Operation(
            summary = "GET /api/v1/notifications/unread-count",
            description = "현재 사용자의 미읽음 알림 개수를 조회합니다"
    )
    @GetMapping("/unread-count")
    public UnreadCountResponse getUnreadCount() {
        return notificationQuery.countUnread();
    }

    @Operation(
            summary = "PUT /api/v1/notifications/{notificationId}/read",
            description = "알림 1건을 읽음 처리합니다"
    )
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<Void> readNotification(
            @PathVariable UUID notificationId
    ) {
        markNotificationReadUseCase.execute(notificationId);
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
    public ResponseEntity<StreamingResponseBody> stream(
) {
        NotificationStreamSession session = notificationStreamUseCase.connect();

        StreamingResponseBody body = outputStream -> {
            try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
                writer.write("event: connected\ndata: {}\n\n");
                writer.flush();

                while (!Thread.currentThread().isInterrupted()) {
                    String data = session.queue().poll(30, TimeUnit.SECONDS);
                    if (data == null) {
                        writer.write(": keepalive\n\n");
                    } else {
                        writer.write(data);
                    }
                    writer.flush();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                notificationStreamUseCase.disconnect(session);
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .header("X-Accel-Buffering", "no")
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(body);
    }
}
