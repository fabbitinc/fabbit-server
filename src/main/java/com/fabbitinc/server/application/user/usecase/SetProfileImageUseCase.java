package com.fabbitinc.server.application.user.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.file.service.FileService;
import com.fabbitinc.server.application.user.service.UserService;
import com.fabbitinc.server.application.user.usecase.command.SetUserProfileImageCommand;
import com.fabbitinc.server.application.user.usecase.result.SetUserProfileImageResult;
import com.fabbitinc.server.domain.file.model.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SetProfileImageUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final FileService fileService;
    private final UserService userService;
    private final FileUrlResolver fileUrlResolver;

    public SetUserProfileImageResult execute(SetUserProfileImageCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();

        List<File> files = fileService.validateAttachable(List.of(command.fileId()));
        File file = files.getFirst();
        fileService.convertToThumbnail(file);

        userService.setProfileImage(auth.userId(), file);
        log.atInfo()
                .addKeyValue("event.name", "user.profile-image.updated")
                .addKeyValue("user.id", auth.userId())
                .addKeyValue("file.id", file.getId())
                .addKeyValue("outcome", "success")
                .log("user profile image updated");
        return new SetUserProfileImageResult(fileUrlResolver.resolve(file.getFileKey()));
    }
}
