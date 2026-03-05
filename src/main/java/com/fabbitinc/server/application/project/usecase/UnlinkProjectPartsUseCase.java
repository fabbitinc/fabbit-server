package com.fabbitinc.server.application.project.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.project.service.ProjectService;
import com.fabbitinc.server.application.project.usecase.command.UnlinkProjectPartsCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class UnlinkProjectPartsUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final ProjectService projectService;

    public void execute(UnlinkProjectPartsCommand command) {
        currentAuthProvider.getCurrentAuth();
        projectService.unlinkParts(command.projectId(), command.partIds());
    }
}
