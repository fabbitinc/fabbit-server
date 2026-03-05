package com.fabbitinc.server.application.project.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.project.service.ProjectService;
import com.fabbitinc.server.application.project.usecase.command.AddProjectMembersCommand;
import com.fabbitinc.server.application.project.usecase.result.AddProjectMembersResult;
import com.fabbitinc.server.domain.project.model.ProjectRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class AddProjectMembersUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final ProjectService projectService;

    public AddProjectMembersResult execute(AddProjectMembersCommand command) {
        currentAuthProvider.getCurrentAuth();
        ProjectRole actualRole = command.role() == null ? ProjectRole.MEMBER : command.role();
        int count = projectService.addMembers(command.projectId(), command.userIds(), actualRole);
        return new AddProjectMembersResult(count);
    }
}
