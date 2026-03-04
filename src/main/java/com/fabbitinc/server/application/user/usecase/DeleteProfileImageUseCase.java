package com.fabbitinc.server.application.user.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
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

    private final AuthTokenParser authTokenParser;
    private final FileService fileService;
    private final UserService userService;

    @Transactional
    public void execute(String authorizationHeader) {
        AuthContext auth = authTokenParser.requireAuth(authorizationHeader);

        List<File> files = fileService.getFilesByOwner("user", auth.userId());
        if (files.isEmpty()) {
            return;
        }

        userService.deleteProfileImage(auth.userId());
        fileService.softDelete(files.getFirst().getId());
    }
}
