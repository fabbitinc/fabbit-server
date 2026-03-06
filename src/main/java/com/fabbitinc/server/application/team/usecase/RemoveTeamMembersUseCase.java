package com.fabbitinc.server.application.team.usecase;

import com.fabbitinc.server.application.auth.support.CurrentAuthProvider;
import com.fabbitinc.server.application.team.service.TeamService;
import com.fabbitinc.server.application.team.usecase.command.RemoveTeamMembersCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class RemoveTeamMembersUseCase {

    private final CurrentAuthProvider currentAuthProvider;
    private final TeamService teamService;

    public void execute(RemoveTeamMembersCommand command) {
        currentAuthProvider.getCurrentAuth();
        teamService.removeMembers(command.teamId(), command.userIds());
    }
}
