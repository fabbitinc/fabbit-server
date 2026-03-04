package com.fabbitinc.server.application.user.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.file.service.FileService;
import com.fabbitinc.server.application.user.service.UserService;
import com.fabbitinc.server.domain.file.model.File;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DeleteProfileImageUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final FileService fileService;
    private final UserService userService;

    @Transactional
    public void execute() {
        AuthContext auth = currentAuthProvider.getCurrentAuth();

        List<File> files = fileService.getFilesByOwner("user", auth.userId());
        if (files.isEmpty()) {
            return;
        }

        userService.deleteProfileImage(auth.userId());
        fileService.softDelete(files.getFirst().getId());
    }
}
