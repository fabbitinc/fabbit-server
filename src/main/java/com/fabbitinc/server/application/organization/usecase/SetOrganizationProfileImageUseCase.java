package com.fabbitinc.server.application.organization.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.AuthTokenParser;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.file.service.FileService;
import com.fabbitinc.server.application.organization.dto.response.ProfileImageResponse;
import com.fabbitinc.server.application.organization.service.OrganizationService;
import com.fabbitinc.server.domain.file.model.File;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SetOrganizationProfileImageUseCase {

    private final AuthTokenParser authTokenParser;
    private final FileService fileService;
    private final OrganizationService organizationService;
    private final FileUrlResolver fileUrlResolver;

    @Transactional
    public ProfileImageResponse execute(String authorizationHeader, UUID fileId) {
        AuthContext auth = authTokenParser.requireAdmin(authorizationHeader);

        List<File> files = fileService.validateAttachable(List.of(fileId));
        File file = files.getFirst();
        fileService.convertToThumbnail(file);

        organizationService.setProfileImage(auth, file);

        return new ProfileImageResponse(fileUrlResolver.resolve(file.getFileKey()));
    }
}
