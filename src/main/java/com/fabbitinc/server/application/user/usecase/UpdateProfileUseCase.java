package com.fabbitinc.server.application.user.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.user.service.UserService;
import com.fabbitinc.server.application.user.usecase.command.UpdateProfileCommand;
import com.fabbitinc.server.application.user.usecase.result.UpdateProfileResult;
import com.fabbitinc.server.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UpdateProfileUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final UserService userService;
    private final FileUrlResolver fileUrlResolver;

    public UpdateProfileResult execute(UpdateProfileCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();

        userService.updateProfile(auth.userId(), command.fullName(), command.phone());
        User user = userService.getUserOrThrow(auth.userId());

        log.atInfo()
                .addKeyValue("event.name", "user.profile.updated")
                .addKeyValue("user.id", user.getId())
                .addKeyValue("outcome", "success")
                .log("user profile updated");

        return new UpdateProfileResult(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                fileUrlResolver.resolve(user.getProfileImageFileKey()),
                user.getUpdatedAt()
        );
    }
}
