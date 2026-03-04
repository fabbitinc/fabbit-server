package com.fabbitinc.server.application.user.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.user.dto.request.ChangePasswordRequest;
import com.fabbitinc.server.application.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ChangePasswordUseCase {

    private final AuthTokenParser authTokenParser;
    private final UserService userService;

    @Transactional
    public void execute(String authorizationHeader, ChangePasswordRequest request) {
        AuthContext auth = authTokenParser.requireAuth(authorizationHeader);
        userService.changePassword(auth.userId(), request.currentPassword(), request.newPassword());
    }
}
