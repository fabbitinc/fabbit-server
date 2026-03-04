package com.fabbitinc.server.application.user.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.file.service.FileService;
import com.fabbitinc.server.application.user.dto.response.ProfileImageResponse;
import com.fabbitinc.server.application.user.service.UserService;
import com.fabbitinc.server.domain.file.model.File;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SetProfileImageUseCase {

    private final AuthTokenParser authTokenParser;
    private final FileService fileService;
    private final UserService userService;
    private final FileUrlResolver fileUrlResolver;

    @Transactional
    public ProfileImageResponse execute(String authorizationHeader, UUID fileId) {
        AuthContext auth = authTokenParser.requireAuth(authorizationHeader);

        List<File> files = fileService.validateAttachable(List.of(fileId));
        File file = files.getFirst();
        fileService.convertToThumbnail(file);

        userService.setProfileImage(auth.userId(), file);
        return new ProfileImageResponse(fileUrlResolver.resolve(file.getFileKey()));
    }
}
