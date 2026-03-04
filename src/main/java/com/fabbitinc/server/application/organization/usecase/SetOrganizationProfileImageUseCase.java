package com.fabbitinc.server.application.organization.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.common.support.FileUrlResolver;
import com.fabbitinc.server.application.file.service.FileService;
import com.fabbitinc.server.application.organization.dto.response.ProfileImageResponse;
import com.fabbitinc.server.application.organization.service.OrganizationService;
import com.fabbitinc.server.domain.file.model.File;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SetOrganizationProfileImageUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final FileService fileService;
    private final OrganizationService organizationService;
    private final FileUrlResolver fileUrlResolver;

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ProfileImageResponse execute(UUID fileId) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();

        List<File> files = fileService.validateAttachable(List.of(fileId));
        File file = files.getFirst();
        fileService.convertToThumbnail(file);

        organizationService.setProfileImage(auth, file);

        return new ProfileImageResponse(fileUrlResolver.resolve(file.getFileKey()));
    }
}
