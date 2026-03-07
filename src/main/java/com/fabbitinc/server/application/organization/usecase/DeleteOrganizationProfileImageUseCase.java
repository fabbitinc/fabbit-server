package com.fabbitinc.server.application.organization.usecase;

import com.fabbitinc.server.application.auth.support.AuthContext;
import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.file.service.FileService;
import com.fabbitinc.server.application.organization.service.OrganizationService;
import com.fabbitinc.server.application.organization.usecase.command.DeleteOrganizationProfileImageCommand;
import com.fabbitinc.server.domain.file.model.File;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class DeleteOrganizationProfileImageUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final FileService fileService;
    private final OrganizationService organizationService;

    @PreAuthorize("hasRole('ADMIN')")
    public void execute(DeleteOrganizationProfileImageCommand command) {
        AuthContext auth = currentAuthProvider.getCurrentAuth();

        List<File> files = fileService.getFilesByOwner("organization", auth.orgId());
        if (files.isEmpty()) {
            return;
        }

        organizationService.deleteProfileImage(auth);
        fileService.softDelete(files.getFirst().getId());
    }
}
