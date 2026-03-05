package com.fabbitinc.server.application.user.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.user.service.UserService;
import com.fabbitinc.server.application.user.usecase.command.ChangePasswordCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChangePasswordUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final UserService userService;

    public void execute(ChangePasswordCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        userService.changePassword(auth.userId(), command.currentPassword(), command.newPassword());
        log.atInfo()
                .addKeyValue("event.name", "user.password.changed")
                .addKeyValue("user.id", auth.userId())
                .addKeyValue("outcome", "success")
                .log("user password changed");
    }
}
