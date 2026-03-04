package com.fabbitinc.server.application.user.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.user.dto.request.ChangePasswordRequest;
import com.fabbitinc.server.application.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ChangePasswordUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final UserService userService;

    @Transactional
    public void execute(ChangePasswordRequest request) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();
        userService.changePassword(auth.userId(), request.currentPassword(), request.newPassword());
    }
}
