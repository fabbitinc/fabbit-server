package com.fabbitinc.server.application.team.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.team.service.TeamService;
import com.fabbitinc.server.application.team.usecase.command.AddTeamMembersCommand;
import com.fabbitinc.server.application.team.usecase.result.AddTeamMembersResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class AddTeamMembersUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final TeamService teamService;

    public AddTeamMembersResult execute(AddTeamMembersCommand command) {
        currentAuthProvider.getCurrentAuth();
        int count = teamService.addMembers(command.teamId(), command.userIds());
        return new AddTeamMembersResult(count);
    }
}
