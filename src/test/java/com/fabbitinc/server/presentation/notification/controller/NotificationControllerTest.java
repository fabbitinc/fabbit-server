package com.fabbitinc.server.presentation.notification.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fabbitinc.server.application.notification.query.NotificationQuery;
import com.fabbitinc.server.application.notification.usecase.MarkAllNotificationsReadUseCase;
import com.fabbitinc.server.application.notification.usecase.MarkNotificationReadUseCase;
import com.fabbitinc.server.application.notification.usecase.NotificationStreamUseCase;
import com.fabbitinc.server.application.notification.usecase.result.NotificationStreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationQuery notificationQuery;
    @Mock
    private MarkNotificationReadUseCase markNotificationReadUseCase;
    @Mock
    private MarkAllNotificationsReadUseCase markAllNotificationsReadUseCase;
    @Mock
    private NotificationStreamUseCase notificationStreamUseCase;

    @InjectMocks
    private NotificationController notificationController;

    @Test
    void stream_응답쓰기실패시_예외를_전파하지_않고_disconnect한다() {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(1);
        queue.add("data: hello\n\n");
        NotificationStreamResult result = new NotificationStreamResult(UUID.randomUUID(), queue);
        when(notificationStreamUseCase.execute()).thenReturn(result);

        ResponseEntity<StreamingResponseBody> response = notificationController.stream();

        assertNotNull(response.getBody());
        assertDoesNotThrow(() -> response.getBody().writeTo(new FailingOutputStream()));
        verify(notificationStreamUseCase).disconnect(result);
    }

    private static final class FailingOutputStream extends OutputStream {

        @Override
        public void write(int b) throws IOException {
            throw new IOException("client closed");
        }
    }
}
