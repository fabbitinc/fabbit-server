package com.fabbitinc.server.application.notification.usecase;

import com.fabbitinc.server.application.common.exception.AppException;
import com.fabbitinc.server.application.common.exception.ErrorCode;
import com.fabbitinc.server.application.notification.support.SseManager;
import com.fabbitinc.server.application.notification.usecase.command.PushNotificationStreamCommand;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@RequiredArgsConstructor
public class PushNotificationStreamUseCase {

    private final SseManager sseManager;
    private final ObjectMapper objectMapper;

    public void execute(PushNotificationStreamCommand command) {
        sseManager.push(command.userId(), toSseEvent(command));
    }

    private String toSseEvent(PushNotificationStreamCommand command) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("notificationId", command.notificationId().toString());
        payload.put("userId", command.userId().toString());
        try {
            return "event: notification.created\ndata: "
                    + objectMapper.writeValueAsString(payload)
                    + "\n\n";
        } catch (JacksonException ex) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "알림 SSE payload 직렬화에 실패했습니다");
        }
    }
}
