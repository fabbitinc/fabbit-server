package com.fabbitinc.server.application.organization.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.file.service.FileService;
import com.fabbitinc.server.application.organization.service.OrganizationService;
import com.fabbitinc.server.domain.file.model.File;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DeleteOrganizationProfileImageUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final FileService fileService;
    private final OrganizationService organizationService;

    @Transactional
    public void execute() {
        AuthContext auth = currentAuthProvider.getAdminAuth();

        List<File> files = fileService.getFilesByOwner("organization", auth.orgId());
        if (files.isEmpty()) {
            return;
        }

        organizationService.deleteProfileImage(auth);
        fileService.softDelete(files.getFirst().getId());
    }
}
