package com.fabbitinc.server.application.notification.eventhandler;

import com.fabbitinc.server.application.engineeringchange.event.EngineeringChangeReleasedEvent;
import com.fabbitinc.server.application.notification.usecase.CreateReleaseNotificationsUseCase;
import com.fabbitinc.server.application.notification.usecase.command.CreateReleaseNotificationsCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EngineeringChangeReleasedEventHandler {

    private final CreateReleaseNotificationsUseCase createReleaseNotificationsUseCase;

    @EventListener
    public void handle(EngineeringChangeReleasedEvent event) {
        log.debug(
                "event=engineering_change_released event_type={} aggregate_id={}",
                event.getClass().getSimpleName(),
                event.engineeringChangeId()
        );
        createReleaseNotificationsUseCase.execute(new CreateReleaseNotificationsCommand(
                event.engineeringChangeId(),
                event.actorId(),
                event.ecNumber(),
                event.ecTitle()
        ));
    }
}
