package com.fabbitinc.server.application.user.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.file.service.FileService;
import com.fabbitinc.server.application.user.service.UserService;
import com.fabbitinc.server.application.user.usecase.command.DeleteUserProfileImageCommand;
import com.fabbitinc.server.domain.file.model.File;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DeleteProfileImageUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final FileService fileService;
    private final UserService userService;

    public void execute(DeleteUserProfileImageCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();

        List<File> files = fileService.getFilesByOwner("user", auth.userId());
        if (files.isEmpty()) {
            return;
        }

        userService.deleteProfileImage(auth.userId());
        fileService.softDelete(files.getFirst().getId());
        log.atInfo()
                .addKeyValue("event.name", "user.profile-image.deleted")
                .addKeyValue("user.id", auth.userId())
                .addKeyValue("file.id", files.getFirst().getId())
                .addKeyValue("outcome", "success")
                .log("user profile image deleted");
    }
}
